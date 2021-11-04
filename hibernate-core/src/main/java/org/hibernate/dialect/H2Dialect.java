/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import jakarta.persistence.TemporalType;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.query.FetchClauseType;
import org.hibernate.query.IntervalType;
import org.hibernate.query.NullOrdering;
import org.hibernate.PessimisticLockException;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.hint.IndexQueryHintHandler;
import org.hibernate.dialect.identity.H2IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.H2SequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.internal.idtable.AfterUseAction;
import org.hibernate.query.sqm.mutation.internal.idtable.IdTable;
import org.hibernate.query.sqm.mutation.internal.idtable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorH2DatabaseImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.jboss.logging.Logger;

import static org.hibernate.query.TemporalUnit.SECOND;

/**
 * A dialect compatible with the H2 database.
 *
 * @author Thomas Mueller
 */
public class H2Dialect extends Dialect {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			H2Dialect.class.getName()
	);

	private final LimitHandler limitHandler;

	private final boolean cascadeConstraints;
	private final boolean useLocalTime;

	private final int version;

	private final boolean supportsTuplesInSubqueries;
	private final SequenceInformationExtractor sequenceInformationExtractor;
	private final String querySequenceString;

	public H2Dialect(DialectResolutionInfo info) {
		this(
				info.getDatabaseMajorVersion() * 100000
						+ info.getDatabaseMinorVersion() * 1000,
				parseBuildId( info )
		);
		registerKeywords( info );
	}

	public H2Dialect() {
		this(0, 0);
	}

	public H2Dialect(int version, int buildId) {
		this(version + buildId);
	}

	public H2Dialect(int version) {
		super();
		this.version = version;
		// https://github.com/h2database/h2database/commit/b2cdf84e0b84eb8a482fa7dccdccc1ab95241440
		limitHandler = version >= 104195
				? OffsetFetchLimitHandler.INSTANCE
				: LimitOffsetLimitHandler.INSTANCE;

		if ( version < 102139 ) {
			final int majorVersion = version / 100000;
			final int minorVersion = version % 100000 / 1000;
			final int buildId = version % 1000;
			LOG.unsupportedMultiTableBulkHqlJpaql( majorVersion, minorVersion, buildId );
		}
		supportsTuplesInSubqueries = version >= 104198;
		// Prior to 1.4.200 the 'cascade' in 'drop table' was implicit
		cascadeConstraints = version >= 104200;
		// 1.4.200 introduced changes in current_time and current_timestamp
		useLocalTime = version >= 104199;

		getDefaultProperties().setProperty( AvailableSettings.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		// http://code.google.com/p/h2database/issues/detail?id=235
		getDefaultProperties().setProperty( AvailableSettings.NON_CONTEXTUAL_LOB_CREATION, "true" );

		if ( version >= 104032 ) {
			this.sequenceInformationExtractor = version >= 104201
					? SequenceInformationExtractorLegacyImpl.INSTANCE
					: SequenceInformationExtractorH2DatabaseImpl.INSTANCE;
			this.querySequenceString = "select * from INFORMATION_SCHEMA.SEQUENCES";
			registerColumnType( Types.DECIMAL,  "numeric($p,$s)" );
		}
		else {
			this.sequenceInformationExtractor = SequenceInformationExtractorNoOpImpl.INSTANCE;
			this.querySequenceString = null;
		}

		if ( version < 200 ) {
			// prior to version 2.0, H2 reported NUMERIC columns as DECIMAL,
			// which caused problems for schema update tool
			registerColumnType( Types.NUMERIC, "decimal($p,$s)" );
		}

		registerColumnType( SqlTypes.UUID, "uuid" );
		registerColumnType( SqlTypes.INTERVAL_SECOND, "interval second($p,$s)" );
		registerColumnType( SqlTypes.GEOMETRY, "geometry" );
		registerColumnType( SqlTypes.ARRAY, "array" );
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeDescriptorRegistry();
		jdbcTypeRegistry.addDescriptorIfAbsent( UUIDJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptorIfAbsent( DurationIntervalSecondJdbcType.INSTANCE );
	}

	private static int parseBuildId(DialectResolutionInfo info) {
		final String databaseVersion = info.getDatabaseVersion();
		if ( databaseVersion == null ) {
			return 0;
		}

		final String[] bits = databaseVersion.split("[. ]");
		return bits.length > 2 ? Integer.parseInt( bits[2] ) : 0;
	}

	public boolean hasDstBug() {
		// H2 1.4.200 has a bug: https://github.com/h2database/h2database/issues/3184
		return getVersion() == 104200;
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		// H2 needs an actual argument type for aggregates like SUM, AVG, MIN, MAX to determine the result type
		CommonFunctionFactory.aggregates( this, queryEngine, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );

		CommonFunctionFactory.pi( queryEngine );
		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.radians( queryEngine );
		CommonFunctionFactory.degrees( queryEngine );
		CommonFunctionFactory.log10( queryEngine );
		CommonFunctionFactory.rand( queryEngine );
		CommonFunctionFactory.truncate( queryEngine );
		CommonFunctionFactory.soundex( queryEngine );
		CommonFunctionFactory.translate( queryEngine );
		CommonFunctionFactory.bitand( queryEngine );
		CommonFunctionFactory.bitor( queryEngine );
		CommonFunctionFactory.bitxor( queryEngine );
		CommonFunctionFactory.bitAndOr( queryEngine );
		CommonFunctionFactory.yearMonthDay( queryEngine );
		CommonFunctionFactory.hourMinuteSecond( queryEngine );
		CommonFunctionFactory.dayOfWeekMonthYear( queryEngine );
		CommonFunctionFactory.weekQuarter( queryEngine );
		CommonFunctionFactory.daynameMonthname( queryEngine );
		CommonFunctionFactory.localtimeLocaltimestamp( queryEngine );
		CommonFunctionFactory.bitLength( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.ascii( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.space( queryEngine );
		CommonFunctionFactory.repeat( queryEngine );
		CommonFunctionFactory.chr_char( queryEngine );
		CommonFunctionFactory.instr( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		//also natively supports ANSI-style substring()
		CommonFunctionFactory.position( queryEngine );
		CommonFunctionFactory.trim1( queryEngine );
		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.nowCurdateCurtime( queryEngine );
		CommonFunctionFactory.sysdate( queryEngine );
		CommonFunctionFactory.insert( queryEngine );
//		CommonFunctionFactory.everyAny( queryEngine ); //this would work too
		CommonFunctionFactory.everyAny_boolAndOr( queryEngine );
		CommonFunctionFactory.median( queryEngine );
		CommonFunctionFactory.stddevPopSamp( queryEngine );
		CommonFunctionFactory.varPopSamp( queryEngine );
		if ( version == 104200 ) {
			// See https://github.com/h2database/h2database/issues/2518
			CommonFunctionFactory.format_toChar( queryEngine );
		}
		else {
			CommonFunctionFactory.format_formatdatetime( queryEngine );
		}
		CommonFunctionFactory.rownum( queryEngine );
	}

	@Override
	public String currentTime() {
		return useLocalTime ? "localtime" : super.currentTime();
	}

	@Override
	public String currentTimestamp() {
		return useLocalTime ? "localtimestamp" : super.currentTimestamp();
	}

	@Override
	public String currentTimestampWithTimeZone() {
		return "current_timestamp";
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new H2SqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	/**
	 * In H2, the extract() function does not return
	 * fractional seconds for the the field
	 * {@link TemporalUnit#SECOND}. We work around
	 * this here with two calls to extract().
	 */
	@Override
	public String extractPattern(TemporalUnit unit) {
		return unit == SECOND
				? "(" + super.extractPattern(unit) + "+extract(nanosecond from ?2)/1e9)"
				: super.extractPattern(unit);
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		if ( intervalType != null ) {
			return "(?2+?3)";
		}
		return "dateadd(?1,?2,?3)";
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		return "datediff(?1,?2,?3)";
	}

	@Override
	public boolean supportsTemporalLiteralOffset() {
		return true;
	}

	@Override
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NATIVE;
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool );
	}

	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}

	@Override
	public boolean supportsIfExistsAfterTableName() {
		return !supportsIfExistsBeforeTableName();
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return cascadeConstraints;
	}

	@Override
	public boolean supportsIfExistsAfterAlterTable() {
		return cascadeConstraints;
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return true;
	}

	@Override
	public String getCascadeConstraintsString() {
		return cascadeConstraints ? " cascade "
				: super.getCascadeConstraintsString();
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return H2SequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return querySequenceString;
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return sequenceInformationExtractor;
	}

	@Override
	public NullOrdering getNullOrdering() {
		return NullOrdering.FIRST;
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType entityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new LocalTemporaryTableStrategy(
				new IdTable( entityDescriptor, basename -> "HT_" + basename, this ),
				this::getTypeName,
				AfterUseAction.CLEAN,
				TempTableDdlTransactionHandling.NONE,
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				String constraintName = null;
				// 23000: Check constraint violation: {0}
				// 23001: Unique index or primary key violation: {0}
				if ( sqle.getSQLState().startsWith( "23" ) ) {
					final String message = sqle.getMessage();
					final int idx = message.indexOf( "violation: " );
					if ( idx > 0 ) {
						constraintName = message.substring( idx + "violation: ".length() );
					}
					if ( sqle.getSQLState().equals( "23506" ) ) {
						constraintName = constraintName.substring( 1, constraintName.indexOf( ":" ) );
					}
				}
				return constraintName;
			} );

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );

			switch (errorCode) {
				case 40001:
					// DEADLOCK DETECTED
					return new LockAcquisitionException(message, sqlException, sql);
				case 50200:
					// LOCK NOT AVAILABLE
					return new PessimisticLockException(message, sqlException, sql);
				case 90006:
					// NULL not allowed for column [90006-145]
					final String constraintName = getViolatedConstraintNameExtractor().extractConstraintName(sqlException);
					return new ConstraintViolationException(message, sqlException, sql, constraintName);
			}

			return null;
		};
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "call current_timestamp()";
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public boolean supportsTupleCounts() {
		return true;
	}

	@Override
	public boolean requiresParensForTupleDistinctCounts() {
		return true;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		// see http://groups.google.com/group/h2-database/browse_thread/thread/562d8a49e2dabe99?hl=en
		return true;
	}

	@Override
	public SelectItemReferenceStrategy getGroupBySelectItemReferenceStrategy() {
		return SelectItemReferenceStrategy.ALIAS;
	}

	@Override
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	public boolean supportsFetchClause(FetchClauseType type) {
		return getVersion() >= 104198;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new H2IdentityColumnSupport();
	}

	@Override
	public String getQueryHintString(String query, String hints) {
		return IndexQueryHintHandler.INSTANCE.addQueryHints( query, hints );
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		if ( version == 104200 ) {
			// See https://github.com/h2database/h2database/issues/2518
			appender.appendSql( OracleDialect.datetimeFormat( format, true, true ).result() );
		}
		else {
			appender.appendSql(
					new Replacer( format, "'", "''" )
					.replace("e", "u")
					.replace( "xxx", "XXX" )
					.replace( "xx", "XX" )
					.replace( "x", "X" )
					.result()
			);
		}
	}

	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_MONTH: return "day";
			case WEEK: return "iso_week";
			default: return unit.toString();
		}
	}

}
