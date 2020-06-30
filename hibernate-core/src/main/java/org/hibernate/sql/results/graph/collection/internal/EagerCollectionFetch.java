/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.internal.fk.ForeignKey;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class EagerCollectionFetch extends CollectionFetch implements FetchParent {
	private final DomainResult keyContainerResult;
	private final DomainResult keyCollectionResult;

	private final Fetch elementFetch;
	private final Fetch indexFetch;

//	private final List<Fetch> fetches;

	private final CollectionInitializerProducer initializerProducer;

	public EagerCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			TableGroup collectionTableGroup,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		super( fetchedPath, fetchedAttribute, fetchParent );

		final FromClauseAccess fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();
		final NavigablePath parentPath = fetchedPath.getParent();
		final TableGroup parentTableGroup = parentPath == null ? null : fromClauseAccess.findTableGroup( parentPath );

		final ForeignKey keyDescriptor = fetchedAttribute.getForeignKeyDescriptor();
		if ( parentTableGroup != null ) {
			// join fetch
			keyContainerResult = keyDescriptor.getTargetSide().getKeyPart().createDomainResult( fetchedPath, parentTableGroup, null, creationState );
			keyCollectionResult = keyDescriptor.getReferringSide().getKeyPart().createDomainResult(
					fetchedPath,
					collectionTableGroup,
					null,
					creationState
			);
		}
		else {
			// select fetch
			// todo (6.0) : we could potentially leverage batch fetching for performance
			keyContainerResult = keyDescriptor.getTargetSide().getKeyPart().createDomainResult( fetchedPath, parentTableGroup, null, creationState );

			// use null for `keyCollectionResult`... the initializer will see that as trigger to use
			// the assembled container-key value as the collection-key value.
			keyCollectionResult = null;
		}

		if ( fetchedAttribute.getIndexDescriptor() != null ) {
			indexFetch = creationState.buildKeyFetch( this );
		}
		else {
			indexFetch = null;
		}

		final List<Fetch> fetches = creationState.buildFetches( this );
		if ( fetches.isEmpty() ) {
			// due to fetch depth limit
			elementFetch = null;
		}
		else {
			elementFetch = fetches.get( 0 );
		}

		final CollectionSemantics collectionSemantics = getFetchedMapping().getCollectionDescriptor().getCollectionSemantics();
		initializerProducer = collectionSemantics.createInitializerProducer(
				fetchedPath,
				fetchedAttribute,
				fetchParent,
				true,
				null,
				// todo (6.0) : we need to propagate these lock modes
				LockMode.READ,
				creationState
		);
	}

	@Override
	public DomainResultAssembler createAssembler(FetchParentAccess parentAccess, AssemblerCreationState creationState) {
		final CollectionInitializer initializer = (CollectionInitializer) creationState.resolveInitializer(
				getNavigablePath(),
				() -> {
					final DomainResultAssembler keyContainerAssembler = keyContainerResult.createResultAssembler( creationState );

					final DomainResultAssembler keyCollectionAssembler;
					if ( keyCollectionResult == null ) {
						keyCollectionAssembler = null;
					}
					else {
						keyCollectionAssembler = keyCollectionResult.createResultAssembler( creationState );
					}

					return initializerProducer.produceInitializer(
							getNavigablePath(),
							getFetchedMapping(),
							parentAccess,
							null,
							keyContainerAssembler,
							keyCollectionAssembler,
							creationState
					);
				}
		);

		return new EagerCollectionAssembler( getFetchedMapping(), initializer );
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	@Override
	public boolean hasTableGroup() {
		return true;
	}

	@Override
	public FetchableContainer getReferencedMappingContainer() {
		return getFetchedMapping();
	}

	@Override
	public PluralAttributeMapping getReferencedMappingType() {
		return getFetchedMapping();
	}

	@Override
	public Fetch getKeyFetch() {
		return indexFetch;
	}

	@Override
	public List<Fetch> getFetches() {
		return Collections.singletonList( elementFetch );
	}

	@Override
	public Fetch findFetch(String fetchableName) {
		if ( CollectionPart.Nature.ELEMENT.getName().equals( fetchableName ) ) {
			return elementFetch;
		}
		else if ( CollectionPart.Nature.INDEX.getName().equals( fetchableName ) ) {
			return indexFetch;
		}
		else {
			throw new IllegalArgumentException(
					"Unknown fetchable [" + getFetchedMapping().getCollectionDescriptor().getRole() +
							" -> " + fetchableName + "]"
			);
		}
	}

	@Override
	public JavaTypeDescriptor getResultJavaTypeDescriptor() {
		return getFetchedMapping().getJavaTypeDescriptor();
	}
}
