/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.internal;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.sql.ast.produce.metamodel.spi.MetamodelSelectBuilder;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractMetamodelSelectBuilder implements MetamodelSelectBuilder {
	private final SessionFactoryImplementor sessionFactory;
	private final NavigableContainer rootNavigable;
	private final Navigable restrictedNavigable;

	public AbstractMetamodelSelectBuilder(
			SessionFactoryImplementor sessionFactory,
			NavigableContainer rootNavigable,
			Navigable restrictedNavigable) {
		this.sessionFactory = sessionFactory;
		this.rootNavigable = rootNavigable;
		this.restrictedNavigable = restrictedNavigable;
	}

	@Override
	public NavigableContainer getRootNavigableContainer() {
		return rootNavigable;
	}

	@Override
	public SqlAstSelectDescriptor generateSelectStatement(
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions) {

		return MetamodelSelectBuilderProcess.createSelect(
				sessionFactory,
				rootNavigable,
				restrictedNavigable,
				// allow passing in a QueryResult (readers) already built
				null,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions
		);
	}
}
