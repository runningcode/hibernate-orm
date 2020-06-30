/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.Collections;
import java.util.List;

import org.hibernate.type.spi.TypeConfiguration;

/**
 * Any basic-typed ValueMapping - e.g. a basic-valued singular attribute or a
 * basic-valued collection element
 *
 * @author Steve Ebersole
 */
public interface BasicValuedMapping extends ValueMapping, SqlExpressable {
	@Override
	default int getJdbcTypeCount(TypeConfiguration typeConfiguration) {
		return 1;
	}

	@Override
	default List<JdbcMapping> getJdbcMappings(TypeConfiguration typeConfiguration) {
		return Collections.singletonList( getJdbcMapping() );
	}

	JdbcMapping getJdbcMapping();
}
