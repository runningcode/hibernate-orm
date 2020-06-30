/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.StateArrayContributorMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.sql.results.graph.FetchOptions;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractStateArrayContributorMapping
		extends AbstractAttributeMapping
		implements StateArrayContributorMapping, FetchOptions {

	private final StateArrayContributorMetadataAccess attributeMetadataAccess;
	private final FetchTiming fetchTiming;
	private final FetchStyle fetchStyle;
	private final int stateArrayPosition;


	public AbstractStateArrayContributorMapping(
			String name,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchTiming fetchTiming,
			FetchStyle fetchStyle,
			int stateArrayPosition,
			ManagedMappingType declaringType) {
		super( name, declaringType );
		this.attributeMetadataAccess = attributeMetadataAccess;
		this.fetchTiming = fetchTiming;
		this.fetchStyle = fetchStyle;
		this.stateArrayPosition = stateArrayPosition;
	}

	public AbstractStateArrayContributorMapping(
			String name,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchOptions fetchOptions,
			int stateArrayPosition,
			ManagedMappingType declaringType) {
		this(
				name,
				attributeMetadataAccess,
				fetchOptions.getTiming(),
				fetchOptions.getStyle(),
				stateArrayPosition,
				declaringType
		);
	}

	@Override
	public int getStateArrayPosition() {
		return stateArrayPosition;
	}

	@Override
	public StateArrayContributorMetadataAccess getAttributeMetadataAccess() {
		return attributeMetadataAccess;
	}

	@Override
	public String getFetchableName() {
		return getAttributeName();
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public FetchStyle getStyle() {
		return fetchStyle;
	}

	@Override
	public FetchTiming getTiming() {
		return fetchTiming;
	}
}
