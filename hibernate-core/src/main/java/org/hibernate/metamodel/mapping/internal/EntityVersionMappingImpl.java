/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.BasicSingularAttribute;
import org.hibernate.metamodel.mapping.ColumnConsumer;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class EntityVersionMappingImpl implements EntityVersionMapping, FetchOptions {
	private final String attributeName;
	private final EntityMappingType declaringType;

	private final String columnTableExpression;
	private final String columnExpression;

	private final BasicType versionBasicType;

	public EntityVersionMappingImpl(
			String attributeName,
			String columnTableExpression,
			String columnExpression,
			BasicType versionBasicType,
			EntityMappingType declaringType) {
		this.attributeName = attributeName;
		this.declaringType = declaringType;

		this.columnTableExpression = columnTableExpression;
		this.columnExpression = columnExpression;

		this.versionBasicType = versionBasicType;
	}

	@Override
	public BasicSingularAttribute getVersionAttribute() {
		return (BasicSingularAttribute) declaringType.findAttributeMapping( attributeName );
	}

	@Override
	public String getContainingTableExpression() {
		return columnTableExpression;
	}

	@Override
	public String getMappedColumnExpression() {
		return columnExpression;
	}

	@Override
	public MappingType getPartMappingType() {
		return versionBasicType;
	}

	@Override
	public MappingType getMappedTypeDescriptor() {
		return versionBasicType;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return versionBasicType.getJdbcMapping();
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return versionBasicType.getJavaTypeDescriptor();
	}

	@Override
	public String getPartName() {
		return attributeName;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return getVersionAttribute().getNavigableRole();
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return declaringType;
	}

	@Override
	public String getFetchableName() {
		return attributeName;
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().findTableGroup( fetchParent.getNavigablePath() );

		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final TableReference columnTableReference = tableGroup.resolveTableReference( columnTableExpression );

		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey( columnTableReference, columnExpression ),
						sqlAstProcessingState -> new ColumnReference(
								columnTableReference,
								columnExpression,
								versionBasicType,
								sqlAstCreationState.getCreationContext().getSessionFactory()
						)
				),
				versionBasicType.getJdbcMapping().getJavaTypeDescriptor(),
				sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
		);

		return new BasicFetch<>(
				sqlSelection.getValuesArrayPosition(),
				fetchParent,
				fetchablePath,
				this,
				false,
				null,
				fetchTiming,
				creationState
		);
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();

		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final TableReference columnTableReference = tableGroup.resolveTableReference( columnTableExpression );

		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey( columnTableReference, columnExpression ),
						sqlAstProcessingState -> new ColumnReference(
								columnTableReference,
								columnExpression,
								versionBasicType,
								sqlAstCreationState.getCreationContext().getSessionFactory()
						)
				),
				versionBasicType.getJdbcMapping().getJavaTypeDescriptor(),
				sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
		);

		//noinspection unchecked
		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				getJavaTypeDescriptor(),
				navigablePath
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {

	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {

	}

	@Override
	public void visitColumns(ColumnConsumer consumer) {

	}

	@Override
	public FetchStyle getStyle() {
		return FetchStyle.JOIN;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}
}
