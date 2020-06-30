/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.mapping.collections;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.MappingMetamodel;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfArrays;
import org.hibernate.testing.orm.domain.gambit.EntityOfSets;
import org.hibernate.testing.orm.domain.gambit.EntityOfMaps;
import org.hibernate.testing.orm.domain.gambit.EntityOfLists;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 * @author Fabio Massimo Ercoli
 */
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@ServiceRegistry
@SessionFactory
@SuppressWarnings("WeakerAccess")
public class PluralAttributeMappingTests {

	@Test
	public void testArrays(SessionFactoryScope scope) {
		final MappingMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityMappingType containerEntityDescriptor = domainModel.getEntityDescriptor( EntityOfArrays.class );

		assertThat( containerEntityDescriptor.getNumberOfAttributeMappings(), is( 2 ) );

		final AttributeMapping arrayOfBasics = containerEntityDescriptor.findAttributeMapping( "arrayOfBasics" );
		assertThat( arrayOfBasics, notNullValue() );
	}

	@Test
	public void testLists(SessionFactoryScope scope) {
		final MappingMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityMappingType containerEntityDescriptor = domainModel.getEntityDescriptor( EntityOfLists.class );

		assertThat( containerEntityDescriptor.getNumberOfAttributeMappings(), is( 7 ) );

		final AttributeMapping listOfBasics = containerEntityDescriptor.findAttributeMapping( "listOfBasics" );
		assertThat( listOfBasics, notNullValue() );

		final AttributeMapping listOfEnums = containerEntityDescriptor.findAttributeMapping( "listOfEnums" );
		assertThat( listOfEnums, notNullValue() );

		final AttributeMapping listOfConvertedBasics = containerEntityDescriptor.findAttributeMapping( "listOfConvertedEnums" );
		assertThat( listOfConvertedBasics, notNullValue() );

		final AttributeMapping listOfComponents = containerEntityDescriptor.findAttributeMapping( "listOfComponents" );
		assertThat( listOfComponents, notNullValue() );

		final AttributeMapping listOfOneToMany = containerEntityDescriptor.findAttributeMapping( "listOfOneToMany" );
		assertThat( listOfOneToMany, notNullValue() );

		final AttributeMapping listOfManyToMany = containerEntityDescriptor.findAttributeMapping( "listOfManyToMany" );
		assertThat( listOfManyToMany, notNullValue() );
	}

	@Test
	public void testSets(SessionFactoryScope scope) {
		final MappingMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityMappingType containerEntityDescriptor = domainModel.getEntityDescriptor( EntityOfSets.class );

		assertThat( containerEntityDescriptor.getNumberOfAttributeMappings(), is( 12 ) );

		final AttributeMapping setOfBasics = containerEntityDescriptor.findAttributeMapping( "setOfBasics" );
		assertThat( setOfBasics, notNullValue() );

		final AttributeMapping sortedSetOfBasics = containerEntityDescriptor.findAttributeMapping( "sortedSetOfBasics" );
		assertThat( sortedSetOfBasics, notNullValue() );

		final AttributeMapping sortedSetOfBasicsWithSortNaturalByDefault = containerEntityDescriptor.findAttributeMapping( "sortedSetOfBasicsWithSortNaturalByDefault" );
		assertThat( sortedSetOfBasicsWithSortNaturalByDefault, notNullValue() );

		final AttributeMapping orderedSetOfBasics = containerEntityDescriptor.findAttributeMapping( "orderedSetOfBasics" );
		assertThat( orderedSetOfBasics, notNullValue() );

		final AttributeMapping setOfEnums = containerEntityDescriptor.findAttributeMapping( "setOfEnums" );
		assertThat( setOfEnums, notNullValue() );

		final AttributeMapping setOfConvertedBasics = containerEntityDescriptor.findAttributeMapping( "setOfConvertedEnums" );
		assertThat( setOfConvertedBasics, notNullValue() );

		final AttributeMapping setOfComponents = containerEntityDescriptor.findAttributeMapping( "setOfComponents" );
		assertThat( setOfComponents, notNullValue() );

		final AttributeMapping extraLazySetOfComponents = containerEntityDescriptor.findAttributeMapping( "extraLazySetOfComponents" );
		assertThat( extraLazySetOfComponents, notNullValue() );

		final AttributeMapping setOfOneToMany = containerEntityDescriptor.findAttributeMapping( "setOfOneToMany" );
		assertThat( setOfOneToMany, notNullValue() );

		final AttributeMapping setOfManyToMany = containerEntityDescriptor.findAttributeMapping( "setOfManyToMany" );
		assertThat( setOfManyToMany, notNullValue() );
	}

	@Test
	public void testMaps(SessionFactoryScope scope) {
		final MappingMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityMappingType containerEntityDescriptor = domainModel.getEntityDescriptor( EntityOfMaps.class );

		assertThat( containerEntityDescriptor.getNumberOfAttributeMappings(), is( 16 ) );

		final PluralAttributeMapping basicByBasic = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "basicByBasic" );
		assertThat( basicByBasic, notNullValue() );
		assertThat( basicByBasic.getForeignKeyDescriptor(), notNullValue() );
		assertThat( basicByBasic.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping sortedBasicByBasic = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "sortedBasicByBasic" );
		assertThat( sortedBasicByBasic, notNullValue() );
		assertThat( sortedBasicByBasic.getForeignKeyDescriptor(), notNullValue() );
		assertThat( sortedBasicByBasic.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping sortedBasicByBasicWithSortNaturalByDefault = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "sortedBasicByBasicWithSortNaturalByDefault" );
		assertThat( sortedBasicByBasicWithSortNaturalByDefault, notNullValue() );
		assertThat( sortedBasicByBasicWithSortNaturalByDefault.getForeignKeyDescriptor(), notNullValue() );
		assertThat( sortedBasicByBasicWithSortNaturalByDefault.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping basicByEnum = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "basicByEnum" );
		assertThat( basicByEnum, notNullValue() );
		assertThat( basicByEnum.getForeignKeyDescriptor(), notNullValue() );
		assertThat( basicByEnum.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping basicByConvertedEnum = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "basicByConvertedEnum" );
		assertThat( basicByConvertedEnum, notNullValue() );
		assertThat( basicByConvertedEnum.getForeignKeyDescriptor(), notNullValue() );
		assertThat( basicByConvertedEnum.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping someStuffByBasic = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "componentByBasic" );
		assertThat( someStuffByBasic, notNullValue() );
		assertThat( someStuffByBasic.getForeignKeyDescriptor(), notNullValue() );
		assertThat( someStuffByBasic.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping basicBySomeStuff = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "basicByComponent" );
		assertThat( basicBySomeStuff, notNullValue() );
		assertThat( basicBySomeStuff.getForeignKeyDescriptor(), notNullValue() );
		assertThat( basicBySomeStuff.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping oneToManyByBasic = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "oneToManyByBasic" );
		assertThat( oneToManyByBasic, notNullValue() );
		assertThat( oneToManyByBasic.getForeignKeyDescriptor(), notNullValue() );
		assertThat( oneToManyByBasic.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping basicByOneToMany = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "basicByOneToMany" );
		assertThat( basicByOneToMany, notNullValue() );
		assertThat( basicByOneToMany.getForeignKeyDescriptor(), notNullValue() );
		assertThat( basicByOneToMany.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping manyToManyByBasic = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "manyToManyByBasic" );
		assertThat( manyToManyByBasic, notNullValue() );
		assertThat( manyToManyByBasic.getForeignKeyDescriptor(), notNullValue() );
		assertThat( manyToManyByBasic.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping sortedManyToManyByBasic = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "sortedManyToManyByBasic" );
		assertThat( sortedManyToManyByBasic, notNullValue() );
		assertThat( sortedManyToManyByBasic.getForeignKeyDescriptor(), notNullValue() );
		assertThat( sortedManyToManyByBasic.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping sortedManyToManyByBasicWithSortNaturalByDefault = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "sortedManyToManyByBasicWithSortNaturalByDefault" );
		assertThat( sortedManyToManyByBasicWithSortNaturalByDefault, notNullValue() );
		assertThat( sortedManyToManyByBasicWithSortNaturalByDefault.getForeignKeyDescriptor(), notNullValue() );
		assertThat( sortedManyToManyByBasicWithSortNaturalByDefault.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping componentByBasicOrdered = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "componentByBasicOrdered" );
		assertThat( componentByBasicOrdered, notNullValue() );
		assertThat( componentByBasicOrdered.getForeignKeyDescriptor(), notNullValue() );
		assertThat( componentByBasicOrdered.getElementDescriptor(), notNullValue() );
		assertThat( componentByBasicOrdered.getOrderByFragment(), notNullValue() );
	}

}
