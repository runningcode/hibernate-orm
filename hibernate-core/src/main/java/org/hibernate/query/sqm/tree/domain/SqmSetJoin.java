/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Set;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.SetPersistentAttribute;
import org.hibernate.query.PathException;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSetJoin;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SqmSetJoin<O, E>
		extends AbstractSqmPluralJoin<O,Set<E>, E>
		implements JpaSetJoin<O, E> {
	public SqmSetJoin(
			SqmFrom<?,O> lhs,
			SetPersistentAttribute<O, E> pluralValuedNavigable,
			String alias,
			SqmJoinType sqmJoinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, pluralValuedNavigable, alias, sqmJoinType, fetched, nodeBuilder );
	}

	@Override
	public SetPersistentAttribute<O,E> getReferencedPathSource() {
		return (SetPersistentAttribute<O, E>) super.getReferencedPathSource();
	}

	@Override
	public SetPersistentAttribute<O,E> getModel() {
		return getReferencedPathSource();
	}

	@Override
	public SetPersistentAttribute<O,E> getAttribute() {
		return getReferencedPathSource();
	}

	@Override
	public SqmSetJoin<O, E> on(JpaExpression<Boolean> restriction) {
		return (SqmSetJoin<O, E>) super.on( restriction );
	}

	@Override
	public SqmSetJoin<O, E> on(Expression<Boolean> restriction) {
		return (SqmSetJoin<O, E>) super.on( restriction );
	}

	@Override
	public SqmSetJoin<O, E> on(JpaPredicate... restrictions) {
		return (SqmSetJoin<O, E>) super.on( restrictions );
	}

	@Override
	public SqmSetJoin<O, E> on(Predicate... restrictions) {
		return (SqmSetJoin<O, E>) super.on( restrictions );
	}

	@Override
	public SqmCorrelatedSetJoin<O, E> createCorrelation() {
		return new SqmCorrelatedSetJoin<>( this );
	}

	@Override
	public <S extends E> SqmTreatedSetJoin<O,E,S> treatAs(Class<S> treatAsType) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatAsType ) );
	}

	@Override
	public <S extends E> SqmTreatedSetJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		return new SqmTreatedSetJoin<>( this, treatTarget, null );
	}

	@Override
	public <X, Y> SqmAttributeJoin<X, Y> fetch(String attributeName) {
		return fetch( attributeName, JoinType.INNER);
	}

	@Override
	public SqmAttributeJoin<O, E> makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmSetJoin<>(
				creationProcessingState.getPathRegistry().findFromByPath( getLhs().getNavigablePath() ),
				getReferencedPathSource(),
				getExplicitAlias(),
				getSqmJoinType(),
				isFetched(),
				nodeBuilder()
		);
	}
}