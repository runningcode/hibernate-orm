/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.collection.internal.ListInitializerProducer;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.internal.ResultsHelper;

/**
 * Hibernate's standard CollectionSemantics for Lists
 *
 * @author Steve Ebersole
 */
public class StandardListSemantics implements CollectionSemantics<List> {
	/**
	 * Singleton access
	 */
	public static final StandardListSemantics INSTANCE = new StandardListSemantics();

	private StandardListSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.LIST;
	}

	@Override
	public Class<List> getCollectionJavaType() {
		return List.class;
	}

	@Override
	public List instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor) {
		return CollectionHelper.arrayList( anticipatedSize );
	}

	@Override
	public Iterator getElementIterator(List rawCollection) {
		return rawCollection.iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitElements(List rawCollection, Consumer action) {
		rawCollection.forEach( action );
	}

	@Override
	public CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			LockMode lockMode,
			DomainResultCreationState creationState) {
		final Fetch elementFetch = ResultsHelper.extractElementFetch( creationState.buildFetches( fetchParent ) );
		final Fetch indexFetch = attributeMapping.getIndexDescriptor().generateFetch(
				fetchParent,
				navigablePath.append( CollectionPart.Nature.INDEX.getName() ),
				FetchTiming.IMMEDIATE,
				selected,
				lockMode,
				null,
				creationState
		);

		return new ListInitializerProducer( attributeMapping, indexFetch, elementFetch );
	}

	@Override
	public PersistentCollection instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentList( session );
	}

	@Override
	public PersistentCollection wrap(
			Object rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentList( session, (List) rawCollection );
	}
}
