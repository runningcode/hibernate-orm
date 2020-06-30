/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.results.graph.FetchOptions;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSingularAttributeMapping
		extends AbstractStateArrayContributorMapping
		implements SingularAttributeMapping {

	private final PropertyAccess propertyAccess;

	public AbstractSingularAttributeMapping(
			String name,
			int stateArrayPosition,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchOptions fetchOptions,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super( name, attributeMetadataAccess, fetchOptions, stateArrayPosition, declaringType );
		this.propertyAccess = propertyAccess;
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return propertyAccess;
	}
}
