/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionElement;
import org.hibernate.metamodel.model.domain.spi.BasicCollectionElement;
import org.hibernate.metamodel.model.domain.spi.ConvertibleNavigable;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.internal.ScalarResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.spi.BasicType;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class BasicCollectionElementImpl<J>
		extends AbstractCollectionElement<J>
		implements BasicCollectionElement<J>, ConvertibleNavigable<J> {
	private static final Logger log = Logger.getLogger( BasicCollectionElementImpl.class );

	private final Column column;
	private final BasicType<J> basicType;
	private final BasicValueConverter valueConverter;

	@SuppressWarnings("unchecked")
	public BasicCollectionElementImpl(
			PersistentCollectionDescriptor descriptor,
			Collection bootCollectionMapping,
			RuntimeModelCreationContext creationContext) {
		super( descriptor );

		final BasicValueMapping simpleElementValueMapping = (BasicValueMapping) bootCollectionMapping.getElement();

		this.column = creationContext.getDatabaseObjectResolver().resolveColumn( simpleElementValueMapping.getMappedColumn() );

		// todo (6.0) : resolve SimpleValue -> BasicType
		this.basicType = ( (BasicValueMapping) bootCollectionMapping.getElement() ).resolveType();

		this.valueConverter = simpleElementValueMapping.resolveValueConverter( creationContext, basicType );

		if ( valueConverter != null ) {
			log.debugf(
					"BasicValueConverter [%s] being applied for basic collection elements : %s",
					valueConverter,
					getNavigableRole()
			);
		}
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return valueConverter;
	}

	@Override
	public ElementClassification getClassification() {
		return ElementClassification.BASIC;
	}

	@Override
	public SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext) {
		return new SqmCollectionElementReferenceBasic( (SqmPluralAttributeReference) containerReference );
	}

	@Override
	public BasicType<J> getBasicType() {
		return basicType;
	}

	@Override
	public Column getBoundColumn() {
		return column;
	}


	@Override
	public DomainResult createDomainResult(
			NavigableReference navigableReference,
			String resultVariable,
			DomainResultCreationContext creationContext,
			DomainResultCreationState creationState) {
		assert this.equals( navigableReference.getNavigable() );
		return new ScalarResultImpl(
				resultVariable,
				creationState.getSqlExpressionResolver().resolveSqlSelection(
						creationState.getSqlExpressionResolver().resolveSqlExpression(
								navigableReference.getColumnReferenceQualifier(),
								getBoundColumn()
						),
						getJavaTypeDescriptor(),
						creationContext.getSessionFactory().getTypeConfiguration()
				),
				getBoundColumn().getExpressableType()
		);
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector) {
		// nothing to do
	}
}
