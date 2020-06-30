/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.internal.FilterHelper;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.loader.ast.spi.Loader;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.fk.ForeignKey;
import org.hibernate.metamodel.mapping.internal.fk.ForeignKeyBasic;
import org.hibernate.metamodel.mapping.ordering.OrderByFragment;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.EntityGraphTraversalState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.ResultGraphLogger;
import org.hibernate.sql.results.graph.collection.internal.CollectionDomainResult;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.StandardEntityGraphTraversalStateImpl;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * Builder for SQL AST trees used by {@link Loader} implementations.
 *
 * @author Steve Ebersole
 * @author Nahtan Xu
 */
public class LoaderSelectBuilder {
	/**
	 * Create an SQL AST select-statement based on matching one-or-more keys
	 *
	 * @param loadable The root Loadable
	 * @param partsToSelect Parts of the Loadable to select.  Null/empty indicates to select the Loadable itself
	 * @param restrictedPart Part to base the where-clause restriction on
	 * @param cachedDomainResult DomainResult to be used.  Null indicates to generate the DomainResult
	 * @param numberOfKeysToLoad How many keys should be accounted for in the where-clause restriction?
	 * @param loadQueryInfluencers Any influencers (entity graph, fetch profile) to account for
	 * @param lockOptions Pessimistic lock options to apply
	 * @param jdbcParameterConsumer Consumer for all JdbcParameter references created
	 * @param sessionFactory The SessionFactory
	 */
	public static SelectStatement createSelect(
			Loadable loadable,
			List<? extends ModelPart> partsToSelect,
			ModelPart restrictedPart,
			DomainResult cachedDomainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer,
			SessionFactoryImplementor sessionFactory) {
		final LoaderSelectBuilder process = new LoaderSelectBuilder(
				sessionFactory,
				loadable,
				partsToSelect,
				restrictedPart,
				cachedDomainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions,
				jdbcParameterConsumer
		);

		return process.generateSelect();
	}

	/**
	 * Create an SQL AST select-statement used for subselect-based CollectionLoader
	 *
	 * @see CollectionLoaderSubSelectFetch
	 *
	 * @param attributeMapping The plural-attribute being loaded
	 * @param subselect The subselect details to apply
	 * @param cachedDomainResult DomainResult to be used.  Null indicates to generate the DomainResult?
	 * @param loadQueryInfluencers Any influencers (entity graph, fetch profile) to account for
	 * @param lockOptions Pessimistic lock options to apply
	 * @param jdbcParameterConsumer Consumer for all JdbcParameter references created
	 * @param sessionFactory The SessionFactory
	 */
	public static SelectStatement createSubSelectFetchSelect(
			PluralAttributeMapping attributeMapping,
			SubselectFetch subselect,
			DomainResult cachedDomainResult,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer,
			SessionFactoryImplementor sessionFactory) {
		final LoaderSelectBuilder process = new LoaderSelectBuilder(
				sessionFactory,
				attributeMapping,
				null,
				attributeMapping.getForeignKeyDescriptor().getReferringSide().getKeyPart(),
				cachedDomainResult,
				-1,
				loadQueryInfluencers,
				lockOptions,
				jdbcParameterConsumer
		);

		return process.generateSelect( subselect );
	}

	private final SqlAstCreationContext creationContext;
	private final Loadable loadable;
	private final List<? extends ModelPart> partsToSelect;
	private final ModelPart restrictedPart;
	private final DomainResult cachedDomainResult;
	private final int numberOfKeysToLoad;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final LockOptions lockOptions;
	private final Consumer<JdbcParameter> jdbcParameterConsumer;
	private final EntityGraphTraversalState entityGraphTraversalState;

	private int fetchDepth;
	private Map<OrderByFragment, TableGroup> orderByFragments;

	private LoaderSelectBuilder(
			SqlAstCreationContext creationContext,
			Loadable loadable,
			List<? extends ModelPart> partsToSelect,
			ModelPart restrictedPart,
			DomainResult cachedDomainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer) {
		this.creationContext = creationContext;
		this.loadable = loadable;
		this.partsToSelect = partsToSelect;
		this.restrictedPart = restrictedPart;
		this.cachedDomainResult = cachedDomainResult;
		this.numberOfKeysToLoad = numberOfKeysToLoad;
		this.loadQueryInfluencers = loadQueryInfluencers;
		if ( loadQueryInfluencers != null
				&& loadQueryInfluencers.getEffectiveEntityGraph() != null
				&& loadQueryInfluencers.getEffectiveEntityGraph().getSemantic() != null ) {
			this.entityGraphTraversalState = new StandardEntityGraphTraversalStateImpl( loadQueryInfluencers.getEffectiveEntityGraph() );
		}
		else {
			this.entityGraphTraversalState = null;
		}
		this.lockOptions = lockOptions != null ? lockOptions : LockOptions.NONE;
		this.jdbcParameterConsumer = jdbcParameterConsumer;
	}

	private SelectStatement generateSelect() {
		final NavigablePath rootNavigablePath = new NavigablePath( loadable.getRootPathName() );

		final QuerySpec rootQuerySpec = new QuerySpec( true );
		final List<DomainResult> domainResults;

		final LoaderSqlAstCreationState sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				this::visitFetches,
				numberOfKeysToLoad > 1,
				creationContext
		);

		final TableGroup rootTableGroup = loadable.createRootTableGroup(
				rootNavigablePath,
				null,
				true,
				lockOptions.getLockMode(),
				sqlAstCreationState.getSqlAliasBaseManager(),
				sqlAstCreationState.getSqlExpressionResolver(),
				() -> rootQuerySpec::applyPredicate,
				creationContext
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( rootNavigablePath, rootTableGroup );

		if ( partsToSelect != null && !partsToSelect.isEmpty() ) {
			domainResults = new ArrayList<>( partsToSelect.size() );
			for ( ModelPart part : partsToSelect ) {
				final NavigablePath navigablePath = rootNavigablePath.append( part.getPartName() );
				domainResults.add(
						part.createDomainResult(
								navigablePath,
								rootTableGroup,
								null,
								sqlAstCreationState
						)
				);
			}
		}
		else {
			// use the one passed to the constructor or create one (maybe always create and pass?)
			//		allows re-use as they can be re-used to save on memory - they
			//		do not share state between
			final DomainResult domainResult;
			if ( this.cachedDomainResult != null ) {
				// used the one passed to the constructor
				domainResult = this.cachedDomainResult;
			}
			else {
				// create one
				domainResult = loadable.createDomainResult(
						rootNavigablePath,
						rootTableGroup,
						null,
						sqlAstCreationState
				);
			}

			domainResults = Collections.singletonList( domainResult );
		}

		final int numberOfKeyColumns = restrictedPart.getJdbcTypeCount(
				creationContext.getDomainModel().getTypeConfiguration()
		);

		applyKeyRestriction(
				rootQuerySpec,
				rootNavigablePath,
				rootTableGroup,
				restrictedPart,
				numberOfKeyColumns,
				jdbcParameterConsumer,
				sqlAstCreationState
		);

		if ( loadable instanceof PluralAttributeMapping ) {
			final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) loadable;
			applyFiltering( rootQuerySpec, rootTableGroup, pluralAttributeMapping );
			applyOrdering( rootTableGroup, pluralAttributeMapping );
		}

		if ( orderByFragments != null ) {
			orderByFragments.forEach(
					(orderByFragment, tableGroup) -> orderByFragment.apply( rootQuerySpec, tableGroup, sqlAstCreationState )
			);
		}

		return new SelectStatement( rootQuerySpec, domainResults );
	}

	private void applyKeyRestriction(
			QuerySpec rootQuerySpec,
			NavigablePath rootNavigablePath,
			TableGroup rootTableGroup,
			ModelPart keyPart,
			int numberOfKeyColumns,
			Consumer<JdbcParameter> jdbcParameterConsumer,
			LoaderSqlAstCreationState sqlAstCreationState) {
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		if ( numberOfKeyColumns == 1 ) {
			assert keyPart instanceof BasicValuedModelPart;
			final BasicValuedModelPart basicKeyPart = (BasicValuedModelPart) keyPart;

			final JdbcMapping jdbcMapping = basicKeyPart.getJdbcMapping();

			final String tableExpression = basicKeyPart.getContainingTableExpression();
			final String columnExpression = basicKeyPart.getMappedColumnExpression();
			final TableReference tableReference = rootTableGroup.resolveTableReference( tableExpression );
			final ColumnReference columnRef = (ColumnReference) sqlExpressionResolver.resolveSqlExpression(
					createColumnReferenceKey( tableReference, columnExpression ),
					p -> new ColumnReference(
							tableReference,
							columnExpression,
							jdbcMapping,
							creationContext.getSessionFactory()
					)
			);

			if ( numberOfKeysToLoad == 1 ) {
				final JdbcParameter jdbcParameter = new JdbcParameterImpl( jdbcMapping );
				jdbcParameterConsumer.accept( jdbcParameter );

				rootQuerySpec.applyPredicate(
						new ComparisonPredicate( columnRef, ComparisonOperator.EQUAL, jdbcParameter )
				);
			}
			else {
				final InListPredicate predicate = new InListPredicate( columnRef );
				for ( int i = 0; i < numberOfKeysToLoad; i++ ) {
					for ( int j = 0; j < numberOfKeyColumns; j++ ) {
						final JdbcParameter jdbcParameter = new JdbcParameterImpl( columnRef.getJdbcMapping() );
						jdbcParameterConsumer.accept( jdbcParameter );
						predicate.addExpression( jdbcParameter );
					}
				}
				rootQuerySpec.applyPredicate( predicate );
			}
		}
		else {
			final List<ColumnReference> columnReferences = new ArrayList<>( numberOfKeyColumns );

			keyPart.visitColumns(
					(containingTableExpression, columnExpression, jdbcMapping) -> {
						final TableReference tableReference = rootTableGroup.resolveTableReference( containingTableExpression );
						columnReferences.add(
								(ColumnReference) sqlExpressionResolver.resolveSqlExpression(
										createColumnReferenceKey( tableReference, columnExpression ),
										p -> new ColumnReference(
												tableReference,
												columnExpression,
												jdbcMapping,
												creationContext.getSessionFactory()
										)
								)
						);
					}
			);

			final SqlTuple tuple = new SqlTuple( columnReferences, keyPart );
			final InListPredicate predicate = new InListPredicate( tuple );

			for ( int i = 0; i < numberOfKeysToLoad; i++ ) {
				final List<JdbcParameter> tupleParams = new ArrayList<>( numberOfKeyColumns );
				for ( int j = 0; j < numberOfKeyColumns; j++ ) {
					final ColumnReference columnReference = columnReferences.get( j );
					final JdbcParameter jdbcParameter = new JdbcParameterImpl( columnReference.getJdbcMapping() );
					jdbcParameterConsumer.accept( jdbcParameter );
					tupleParams.add( jdbcParameter );
				}
				final SqlTuple paramTuple = new SqlTuple( tupleParams, keyPart );
				predicate.addExpression( paramTuple );
			}

			rootQuerySpec.applyPredicate( predicate );
		}
	}

	private void applyFiltering(QuerySpec querySpec, TableGroup tableGroup, PluralAttributeMapping pluralAttributeMapping) {
		final Joinable joinable = pluralAttributeMapping
				.getCollectionDescriptor()
				.getCollectionType()
				.getAssociatedJoinable( creationContext.getSessionFactory() );
		final Predicate filterPredicate = FilterHelper.createFilterPredicate(
				loadQueryInfluencers,
				joinable,
				tableGroup
		);
		if ( filterPredicate != null ) {
			querySpec.applyPredicate( filterPredicate );
		}
		if ( pluralAttributeMapping.getCollectionDescriptor().isManyToMany() ) {
			assert joinable instanceof CollectionPersister;
			final Predicate manyToManyFilterPredicate = FilterHelper.createManyToManyFilterPredicate(
					loadQueryInfluencers,
					(CollectionPersister) joinable,
					tableGroup
			);
			if ( manyToManyFilterPredicate != null ) {
				assert tableGroup.getTableReferenceJoins().size() == 1;
				tableGroup.getTableReferenceJoins().get( 0 ).applyPredicate( manyToManyFilterPredicate );
			}
		}
	}

	private void applyOrdering(TableGroup tableGroup, PluralAttributeMapping pluralAttributeMapping) {
		if ( pluralAttributeMapping.getOrderByFragment() != null ) {
			applyOrdering( tableGroup, pluralAttributeMapping.getOrderByFragment() );
		}

		if ( pluralAttributeMapping.getManyToManyOrderByFragment() != null ) {
			applyOrdering( tableGroup, pluralAttributeMapping.getManyToManyOrderByFragment() );
		}
	}

	private void applyOrdering(TableGroup tableGroup, OrderByFragment orderByFragment) {
		if ( orderByFragments == null ) {
			orderByFragments = new LinkedHashMap<>();
		}
		orderByFragments.put( orderByFragment, tableGroup );
	}

	private List<Fetch> visitFetches(
			FetchParent fetchParent,
			QuerySpec querySpec,
			boolean keyGraphs,
			LoaderSqlAstCreationState creationState) {
		ResultGraphLogger.LOGGER.tracef( "Starting visitation of FetchParent's Fetchables : %s", fetchParent.getNavigablePath() );

		final FetchableContainer referencedMappingContainer = fetchParent.getReferencedMappingContainer();

		if ( keyGraphs ) {
			final Fetchable keyFetchable = referencedMappingContainer.getKeyFetchable();
			if ( keyFetchable != null ) {
				final NavigablePath keyNavPath = fetchParent.getNavigablePath().append( keyFetchable.getFetchableName() );
				final Fetch keyFetch = keyFetchable.generateFetch(
						fetchParent,
						keyNavPath,
						FetchTiming.IMMEDIATE,
						false,
						LockMode.READ,
						null,
						creationState
				);
				return Collections.singletonList( keyFetch );
			}

			return Collections.emptyList();
		}

		final List<Fetch> fetches = new ArrayList<>();

		final BiConsumer<Fetchable, Boolean> processor = createFetchableBiConsumer( fetchParent, querySpec, creationState, fetches );
		referencedMappingContainer.visitFetchables( fetchable -> processor.accept( fetchable, keyGraphs ), null );

		return fetches;
	}

	private BiConsumer<Fetchable, Boolean> createFetchableBiConsumer(
			FetchParent fetchParent,
			QuerySpec querySpec,
			LoaderSqlAstCreationState creationState,
			List<Fetch> fetches) {
		return (fetchable, keyGraphs) -> {
			final NavigablePath parentPath = fetchParent.getNavigablePath();
			final NavigablePath fetchablePath = parentPath.append( fetchable.getFetchableName() );

			final Fetch biDirectionalFetch = fetchable.resolveCircularFetch(
					fetchablePath,
					fetchParent,
					creationState
			);

			if ( biDirectionalFetch != null ) {
				fetches.add( biDirectionalFetch );
				return;
			}

			final LockMode lockMode = LockMode.READ;
			FetchTiming fetchTiming = fetchable.getMappedFetchOptions().getTiming();
			boolean joined = fetchable.getMappedFetchOptions().getStyle() == FetchStyle.JOIN;

			EntityGraphTraversalState.TraversalResult traversalResult = null;

			// 'entity graph' takes precedence over 'fetch profile'
			if ( entityGraphTraversalState != null ) {
				traversalResult = entityGraphTraversalState.traverse( fetchParent, fetchable, keyGraphs );
				fetchTiming = traversalResult.getFetchStrategy();
				joined = traversalResult.isJoined();
			}
			else if ( loadQueryInfluencers.hasEnabledFetchProfiles() ) {
				if ( fetchParent instanceof EntityResultGraphNode ) {
					final EntityResultGraphNode entityFetchParent = (EntityResultGraphNode) fetchParent;
					final EntityMappingType entityMappingType = entityFetchParent.getEntityValuedModelPart().getEntityMappingType();
					final String fetchParentEntityName = entityMappingType.getEntityName();
					final String fetchableRole = fetchParentEntityName + "." + fetchable.getFetchableName();

					for ( String enabledFetchProfileName : loadQueryInfluencers.getEnabledFetchProfileNames() ) {
						final FetchProfile enabledFetchProfile = creationContext.getSessionFactory().getFetchProfile( enabledFetchProfileName );
						final org.hibernate.engine.profile.Fetch profileFetch = enabledFetchProfile.getFetchByRole( fetchableRole );

						fetchTiming = FetchTiming.IMMEDIATE;
						joined = joined || profileFetch.getStyle() == org.hibernate.engine.profile.Fetch.Style.JOIN;
					}
				}
			}

			final Integer maximumFetchDepth = creationContext.getMaximumFetchDepth();

			if ( maximumFetchDepth != null ) {
				if ( fetchDepth == maximumFetchDepth ) {
					joined = false;
				}
				else if ( fetchDepth > maximumFetchDepth ) {
					return;
				}
			}

			try {
				if ( !( fetchable instanceof BasicValuedModelPart ) ) {
					fetchDepth++;
				}
				final Fetch fetch = fetchable.generateFetch(
						fetchParent,
						fetchablePath,
						fetchTiming,
						joined,
						lockMode,
						null,
						creationState
				);
				fetches.add( fetch );

				if ( fetchable instanceof PluralAttributeMapping && fetchTiming == FetchTiming.IMMEDIATE && joined ) {
					final TableGroup joinTableGroup = creationState.getFromClauseAccess().getTableGroup( fetchablePath );
					final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) fetchable;
					applyFiltering(
							querySpec,
							joinTableGroup,
							pluralAttributeMapping
					);
					applyOrdering(
							querySpec,
							fetchablePath,
							pluralAttributeMapping,
							creationState
					);
				}
			}
			finally {
				if ( !( fetchable instanceof BasicValuedModelPart ) ) {
					fetchDepth--;
				}
				if ( entityGraphTraversalState != null ) {
					entityGraphTraversalState.backtrack( traversalResult.getPreviousContext() );
				}
			}
		};
	}

	private void applyOrdering(
			QuerySpec ast,
			NavigablePath navigablePath,
			PluralAttributeMapping pluralAttributeMapping,
			LoaderSqlAstCreationState sqlAstCreationState) {
		assert pluralAttributeMapping.getAttributeName().equals( navigablePath.getLocalName() );

		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup( navigablePath );
		assert tableGroup != null;

		applyOrdering( tableGroup, pluralAttributeMapping );
	}

	private SelectStatement generateSelect(SubselectFetch subselect) {
		// todo (6.0) : i think we may even be able to convert this to a join by piecing together
		//		parts from the subselect-fetch sql-ast..

		// todo (6.0) : ^^ another interesting idea is to use `partsToSelect` here relative to the owner
		//		- so `loadable` is the owner entity-descriptor and the `partsToSelect` is the collection

		assert loadable instanceof PluralAttributeMapping;

		final PluralAttributeMapping attributeMapping = (PluralAttributeMapping) loadable;

		final QuerySpec rootQuerySpec = new QuerySpec( true );

		final NavigablePath rootNavigablePath = new NavigablePath( loadable.getRootPathName() );

		final LoaderSqlAstCreationState sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				this::visitFetches,
				numberOfKeysToLoad > 1,
				creationContext
		);

		// todo (6.0) : I think we want to continue to assign aliases to these table-references.  we just want
		//  	to control how that gets rendered in the walker

		final TableGroup rootTableGroup = loadable.createRootTableGroup(
				rootNavigablePath,
				null,
				true,
				lockOptions.getLockMode(),
				sqlAstCreationState.getSqlAliasBaseManager(),
				sqlAstCreationState.getSqlExpressionResolver(),
				() -> rootQuerySpec::applyPredicate,
				creationContext
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( rootNavigablePath, rootTableGroup );

		// generate and apply the restriction
		applySubSelectRestriction(
				rootQuerySpec,
				rootNavigablePath,
				rootTableGroup,
				subselect,
				sqlAstCreationState
		);

		// NOTE : no need to check - we are explicitly processing a plural-attribute
		applyFiltering( rootQuerySpec, rootTableGroup, attributeMapping );
		applyOrdering( rootTableGroup, attributeMapping );

		// register the jdbc-parameters
		subselect.getLoadingJdbcParameters().forEach( jdbcParameterConsumer );

		return new SelectStatement(
				rootQuerySpec,
				Collections.singletonList(
						new CollectionDomainResult(
								rootNavigablePath,
								attributeMapping,
								null,
								rootTableGroup,
								sqlAstCreationState
						)
				)
		);
	}

	private void applySubSelectRestriction(
			QuerySpec querySpec,
			NavigablePath rootNavigablePath,
			TableGroup rootTableGroup,
			SubselectFetch subselect,
			LoaderSqlAstCreationState sqlAstCreationState) {
		final SqlAstCreationContext sqlAstCreationContext = sqlAstCreationState.getCreationContext();
		final SessionFactoryImplementor sessionFactory = sqlAstCreationContext.getSessionFactory();

		assert loadable instanceof PluralAttributeMapping;
		assert restrictedPart == null || restrictedPart instanceof ForeignKey;

		final PluralAttributeMapping attributeMapping = (PluralAttributeMapping) loadable;
		final ForeignKey fkDescriptor = attributeMapping.getForeignKeyDescriptor();

		final Expression fkExpression;

		final int jdbcTypeCount = fkDescriptor.getReferringSide().getKeyPart().getJdbcTypeCount( sessionFactory.getTypeConfiguration() );
		if ( jdbcTypeCount == 1 ) {
			assert fkDescriptor instanceof ForeignKeyBasic;
			final ForeignKeyBasic simpleFkDescriptor = (ForeignKeyBasic) fkDescriptor;
			final BasicValuedModelPart basicKeyPart = (BasicValuedModelPart) simpleFkDescriptor.getReferringSide().getKeyPart();
			fkExpression = sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
					createColumnReferenceKey(
							basicKeyPart.getContainingTableExpression(),
							basicKeyPart.getMappedColumnExpression()
					),
					sqlAstProcessingState -> new ColumnReference(
							rootTableGroup.resolveTableReference( basicKeyPart.getContainingTableExpression() ),
							basicKeyPart.getMappedColumnExpression(),
							basicKeyPart.getJdbcMapping(),
							this.creationContext.getSessionFactory()
					)
			);
		}
		else {
			final List<ColumnReference> columnReferences = new ArrayList<>( jdbcTypeCount );
			final EmbeddableValuedFetchable basicKeyPart = (EmbeddableValuedFetchable) fkDescriptor.getReferringSide().getKeyPart();
			basicKeyPart.visitColumns(
					(containingTableExpression, columnExpression, jdbcMapping) ->
						columnReferences.add(
								(ColumnReference) sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
										createColumnReferenceKey( containingTableExpression, columnExpression ),
										sqlAstProcessingState -> new ColumnReference(
												rootTableGroup.resolveTableReference( containingTableExpression ),
												columnExpression,
												jdbcMapping,
												this.creationContext.getSessionFactory()
										)
								)
						)
			);

			fkExpression = new SqlTuple( columnReferences, basicKeyPart );
		}

		querySpec.applyPredicate(
				new InSubQueryPredicate(
						fkExpression,
						generateSubSelect(
								attributeMapping,
								rootTableGroup,
								subselect,
								jdbcTypeCount,
								sqlAstCreationState,
								sessionFactory
						),
						false
				)
		);
	}

	private QuerySpec generateSubSelect(
			PluralAttributeMapping attributeMapping,
			TableGroup rootTableGroup,
			SubselectFetch subselect,
			int jdbcTypeCount,
			LoaderSqlAstCreationState creationState,
			SessionFactoryImplementor sessionFactory) {
		final ForeignKey fkDescriptor = attributeMapping.getForeignKeyDescriptor();

		final QuerySpec subQuery = new QuerySpec( false );

		final QuerySpec loadingSqlAst = subselect.getLoadingSqlAst();

		// todo (6.0) : we need to find the owner's TableGroup in the `loadingSqlAst`
		final TableGroup ownerTableGroup = subselect.getOwnerTableGroup();

		// transfer the from-clause
		loadingSqlAst.getFromClause().visitRoots( subQuery.getFromClause()::addRoot );

		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlExpressionResolver();

		final MutableInteger count = new MutableInteger();
		fkDescriptor.visitTargetColumns(
				(containingTableExpression, columnExpression, jdbcMapping) -> {
					// for each column, resolve a SqlSelection and add it to the sub-query select-clause
					final TableReference tableReference = ownerTableGroup.resolveTableReference( containingTableExpression );
					final Expression expression = sqlExpressionResolver.resolveSqlExpression(
							createColumnReferenceKey( tableReference, columnExpression ),
							sqlAstProcessingState -> new ColumnReference(
									tableReference,
									columnExpression,
									jdbcMapping,
									sessionFactory
							)
					);
					final int valuesPosition = count.getAndIncrement();
					subQuery.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									valuesPosition + 1,
									valuesPosition,
									expression
							)
					);
				}
		);

		// transfer the restriction
		subQuery.applyPredicate( loadingSqlAst.getWhereClauseRestrictions() );

		return subQuery;
	}
}

