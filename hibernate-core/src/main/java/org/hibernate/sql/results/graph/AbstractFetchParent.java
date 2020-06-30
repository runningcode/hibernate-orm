/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.Collections;
import java.util.List;

import org.hibernate.query.NavigablePath;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractFetchParent implements FetchParent {
	private final FetchableContainer fetchContainer;
	private final NavigablePath navigablePath;

	protected Fetch keyFetch;
	protected List<Fetch> fetches;

	public AbstractFetchParent(FetchableContainer fetchContainer, NavigablePath navigablePath) {
		this.fetchContainer = fetchContainer;
		this.navigablePath = navigablePath;
	}

	protected void afterInitialize(DomainResultCreationState creationState) {
		this.keyFetch = creationState.buildKeyFetch( this );
		this.fetches = creationState.buildFetches( this );
	}

	public FetchableContainer getFetchContainer() {
		return fetchContainer;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public JavaTypeDescriptor getResultJavaTypeDescriptor() {
		return fetchContainer.getJavaTypeDescriptor();
	}

	@Override
	public FetchableContainer getReferencedMappingContainer() {
		return fetchContainer;
	}

	@Override
	public List<Fetch> getFetches() {
		return fetches == null ? Collections.emptyList() : Collections.unmodifiableList( fetches );
	}

	@Override
	public Fetch findFetch(String fetchableName) {
		if ( fetches != null ) {
			for ( Fetch fetch : fetches ) {
				final Fetchable fetchedMapping = fetch.getFetchedMapping();
				if ( fetchedMapping != null && fetchedMapping
						.getFetchableName().equals( fetchableName ) ) {
					return fetch;
				}
			}
		}
		return null;
	}

	@Override
	public Fetch getKeyFetch() {
		return keyFetch;
	}
}
