/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bootstrap.binding.hbm.cid.nonaggregated.dynamic;

import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.BasicSingularAttribute;
import org.hibernate.metamodel.mapping.internal.NonAggregatedIdentifierMappingImpl;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeTarget;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;

/**
 * Note that this test uses a composite-id with key-many-to-one as part of a
 * dynamic model, which is the main construct needed by hibernate-envers
 *
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = @ServiceRegistry.Setting( name = AvailableSettings.HBM2DDL_AUTO, value = "create-drop" )
)
public class DynamicCompositeIdManyToOneTests {
	@Test
	public void testBinding(ServiceRegistryScope scope) {
		final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) new MetadataSources( scope.getRegistry() )
				.addResource( "org/hibernate/orm/test/bootstrap/binding/hbm/cid/nonaggregated/dynamic/DynamicCompositeIdManyToOne.hbm.xml" )
				.buildMetadata()
				.buildSessionFactory();

		try {
			final EntityPersister entityDescriptor = sessionFactory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.findEntityDescriptor( "DynamicCompositeIdManyToOne" );

			assertThat( entityDescriptor.getNumberOfAttributeMappings(), is( 3 ) );

			final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
			assertThat( identifierMapping, instanceOf( NonAggregatedIdentifierMappingImpl.class ) );
			final NonAggregatedIdentifierMappingImpl cid = (NonAggregatedIdentifierMappingImpl) identifierMapping;
			assertThat( cid.getEmbeddableTypeDescriptor().getNumberOfAttributeMappings(), is( 2 ) );

			final AttributeMapping key1 = cid.getEmbeddableTypeDescriptor().findAttributeMapping( "key1" );
			assertThat( key1, notNullValue() );
			assertThat( key1, instanceOf( BasicSingularAttribute.class ) );

			final AttributeMapping key2 = cid.getEmbeddableTypeDescriptor().findAttributeMapping( "key2" );
			assertThat( key2, notNullValue() );
			assertThat( key2, instanceOf( ToOneAttributeTarget.class ) );

			final AttributeMapping attr1 = entityDescriptor.findAttributeMapping( "attr1" );
			assertThat( attr1, notNullValue() );
			assertThat( attr1, instanceOf( BasicSingularAttribute.class ) );

			assertThat( entityDescriptor.getNumberOfAttributeMappings(), is( 3 ) );

			inTransaction(
					sessionFactory,
					session -> {
						session.createQuery( "select e__ from DynamicCompositeIdManyToOne e__" ).list();
						session.createQuery( "select e__ from DynamicCompositeIdManyToOne e__ where e__.key1 = 1" ).list();
						session.createQuery( "select e__ from DynamicCompositeIdManyToOne e__ where e__.key2.name = 'abc'" ).list();
					}
			);
		}
		finally {
			sessionFactory.close();
		}
	}
}
