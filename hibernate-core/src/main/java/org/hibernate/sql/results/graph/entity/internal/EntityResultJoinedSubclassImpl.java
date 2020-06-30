/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.entity.EntityInitializer;

/**
 * @author Andrea Boriero
 */
public class EntityResultJoinedSubclassImpl extends EntityResultImpl {
	public EntityResultJoinedSubclassImpl(
			NavigablePath navigablePath,
			EntityValuedModelPart entityValuedModelPart,
			String resultVariable,
			DomainResultCreationState creationState) {
		super( navigablePath, entityValuedModelPart, resultVariable, creationState );
	}

	@Override
	public DomainResultAssembler createResultAssembler(AssemblerCreationState creationState) {
		final EntityInitializer initializer = (EntityInitializer) creationState.resolveInitializer(
				getNavigablePath(),
				() -> new EntityResultInitializer(
						this,
						getNavigablePath(),
						getLockMode(),
						getKeyFetch(),
						getDiscriminatorResult(),
						getVersionResult(),
						getRowIdResult(),
						creationState
				)
		);

		return new EntityAssembler( getResultJavaTypeDescriptor(), initializer );
	}

	@Override
	protected EntityDiscriminatorMapping getDiscriminatorMapping(
			EntityMappingType entityDescriptor,
			TableGroup entityTableGroup) {
		final JoinedSubclassEntityPersister joinedSubclassEntityPersister = (JoinedSubclassEntityPersister) entityDescriptor;
		if ( joinedSubclassEntityPersister.hasSubclasses() ) {
			return joinedSubclassEntityPersister.getDiscriminatorMapping( entityTableGroup );
		}
		else {
			return null;
		}
	}
}
