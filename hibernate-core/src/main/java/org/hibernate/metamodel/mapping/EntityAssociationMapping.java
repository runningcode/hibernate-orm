/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.metamodel.mapping.internal.fk.ForeignKeySource;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;

/**
 * Commonality between `many-to-one`, `one-to-one` and `any`, as well as entity-valued collection elements and map-keys
 *
 * @author Steve Ebersole
 */
public interface EntityAssociationMapping extends ForeignKeySource, EntityValuedFetchable {
	@Override
	default String getFetchableName() {
		return getPartName();
	}

	EntityMappingType getAssociatedEntityMappingType();

	/**
	 * The model sub-part relative to the associated entity type that is the target
	 * of this association's foreign-key
	 */
	default ModelPart getKeyTargetMatchPart() {
		return getKeyModelPart().getForeignKeyDescriptor().getTargetSide().getKeyPart();
	}
}
