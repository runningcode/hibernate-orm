/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlTreeCreationLogger;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * Simple implementation of FromClauseAccess
 *
 * @author Steve Ebersole
 */
public class SimpleFromClauseAccessImpl implements FromClauseAccess {
	protected final Map<String, TableGroup> tableGroupMap = new HashMap<>();

	public SimpleFromClauseAccessImpl() {
	}

	@Override
	public TableGroup findTableGroup(NavigablePath navigablePath) {
		return tableGroupMap.get( navigablePath.getIdentifierForTableGroup() );
	}

	@Override
	public void visitTableGroups(BiConsumer<NavigablePath, TableGroup> consumer) {
		tableGroupMap.values().forEach( tableGroup -> consumer.accept( tableGroup.getNavigablePath(), tableGroup ) );
	}

	@Override
	public void registerTableGroup(NavigablePath navigablePath, TableGroup tableGroup) {
		final TableGroup previous = tableGroupMap.put( navigablePath.getIdentifierForTableGroup(), tableGroup );
		SqlTreeCreationLogger.LOGGER.debugf(
				"Registering TableGroup : %s -> %s",
				navigablePath.getFullPath(),
				tableGroup
		);

		if ( previous != null ) {
			SqlTreeCreationLogger.LOGGER.debugf(
					"Registration of TableGroup [%s] for NavigablePath [%s] overrode previous registration : %s",
					tableGroup,
					navigablePath.getFullPath(),
					previous
			);
		}
	}
}
