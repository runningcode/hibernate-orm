/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.List;

import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeTarget;
import org.hibernate.query.NavigablePath;

/**
 * Contract for things that can be the parent of a fetch
 *
 * @author Steve Ebersole
 */
public interface FetchParent extends DomainResultGraphNode {
	/**
	 * This parent's mapping type
	 */
	FetchableContainer getReferencedMappingContainer();

	/**
	 * This parent's mapping type
	 */
	FetchableContainer getReferencedMappingType();

	default FetchParent resolveContainingAssociationParent() {
		final ModelPart referencedModePart = getReferencedModePart();
		if ( referencedModePart instanceof Association ) {
			return this;
		}
		if ( this instanceof Fetch ) {
			( (Fetch) this ).getFetchParent().resolveContainingAssociationParent();
		}
		return null;
	}

	/**
	 * Whereas {@link #getReferencedMappingContainer} and {@link #getReferencedMappingType} return the
	 * referenced container type, this method returns the referenced part.
	 *
	 * E.g. for a many-to-one this methods returns the
	 * {@link ToOneAttributeTarget} while
	 * {@link #getReferencedMappingContainer} and {@link #getReferencedMappingType} return the referenced
	 * {@link org.hibernate.metamodel.mapping.EntityMappingType}.
	 */
	default ModelPart getReferencedModePart() {
		return getReferencedMappingContainer();
	}

	/**
	 * Get the property path to this parent
	 */
	NavigablePath getNavigablePath();

	/**
	 * The fetch for the parent's key, if one.  A key-fetch is either
	 * the Fetch for an entity-identifier or the Fetch of an indexed-collection's index
	 */
	Fetch getKeyFetch();

	/**
	 * Retrieve the fetches owned by this fetch source.
	 */
	List<Fetch> getFetches();

	Fetch findFetch(String fetchableName);
}
