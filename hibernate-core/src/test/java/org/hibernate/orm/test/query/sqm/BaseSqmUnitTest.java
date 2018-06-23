/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;

import org.hibernate.testing.junit5.StandardTags;
import org.junit.jupiter.api.Tag;

/**
 * @author Steve Ebersole
 */
@Tag(StandardTags.SQM)
public abstract class BaseSqmUnitTest
		extends SessionFactoryBasedFunctionalTest
		implements Callback {

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		super.applySettings( builder );

		builder.applySetting( AvailableSettings.JPAQL_STRICT_COMPLIANCE, strictJpaCompliance() );
	}

	protected boolean strictJpaCompliance() {
		return false;
	}

	@Override
	public void registerAfterLoadAction(AfterLoadAction afterLoadAction) {
	}

	protected SqmSelectStatement interpretSelect(String hql) {
		return (SqmSelectStatement) sessionFactoryScope().getSessionFactory()
				.getQueryEngine()
				.getSemanticQueryProducer()
				.interpret( hql );
	}
}
