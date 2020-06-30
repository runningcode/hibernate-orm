/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;

/**
 * @author Chris Cranford
 */
public class ArrayInitializerProducer implements CollectionInitializerProducer {
	private final PluralAttributeMapping arrayDescriptor;
	private final Fetch listIndexFetch;
	private final Fetch elementFetch;

	public ArrayInitializerProducer(
			PluralAttributeMapping arrayDescriptor,
			Fetch listIndexFetch,
			Fetch elementFetch) {
		this.arrayDescriptor = arrayDescriptor;
		this.listIndexFetch = listIndexFetch;
		this.elementFetch = elementFetch;
	}

	@Override
	public CollectionInitializer produceInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResultAssembler<?> keyContainerAssembler,
			DomainResultAssembler<?> keyCollectionAssembler,
			AssemblerCreationState creationState) {
		return new ArrayInitializer(
				navigablePath,
				arrayDescriptor,
				parentAccess,
				lockMode,
				keyContainerAssembler,
				keyCollectionAssembler,
				listIndexFetch.createAssembler( parentAccess, creationState ),
				elementFetch.createAssembler( parentAccess, creationState )
		);
	}

	@Override
	public Fetch getIndexFetch() {
		return listIndexFetch;
	}

	@Override
	public Fetch getElementFetch() {
		return elementFetch;
	}
}
