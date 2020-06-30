/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.graph.Fetchable;

/**
 * @author Steve Ebersole
 */
public interface EntityIdentifierMapping extends ValueMapping, ModelPart, Fetchable {
	String ROLE_LOCAL_NAME = "{id}";

	Object getIdentifier(Object entity, SharedSessionContractImplementor session);
	void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session);
	Object instantiate();

	@Override
	default String getPartName() {
		return ROLE_LOCAL_NAME;
	}
}
