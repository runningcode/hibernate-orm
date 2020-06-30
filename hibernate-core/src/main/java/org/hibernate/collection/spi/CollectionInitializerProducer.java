/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.spi;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;

/**
 * Functional contract to create a CollectionInitializer
 *
 * @author Steve Ebersole
 */
public interface CollectionInitializerProducer {
	/**
	 * todo (6.0) : clean this contract up!
	 */
	CollectionInitializer produceInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResultAssembler<?> keyContainerAssembler,
			DomainResultAssembler<?> keyCollectionAssembler,
			AssemblerCreationState creationState);

	Fetch getIndexFetch();

	Fetch getElementFetch();
}
