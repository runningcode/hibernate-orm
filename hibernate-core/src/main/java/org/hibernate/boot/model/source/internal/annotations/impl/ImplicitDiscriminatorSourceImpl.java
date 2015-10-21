/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.boot.model.source.internal.annotations.impl;

import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.source.internal.annotations.DiscriminatorSource;
import org.hibernate.boot.model.source.internal.annotations.RelationalValueSource;
import org.hibernate.boot.model.source.internal.annotations.metadata.type.EntityTypeMetadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.StandardBasicTypes;

/**
 * @author Steve Ebersole
 */
public class ImplicitDiscriminatorSourceImpl
		extends DiscriminatorSourceSupport
		implements DiscriminatorSource {

	private final EntityTypeMetadata entityTypeMetadata;
	private final RelationalValueSource relationalValueSource;

	public ImplicitDiscriminatorSourceImpl(EntityTypeMetadata entityTypeMetadata) {
		super( entityTypeMetadata );
		this.entityTypeMetadata = entityTypeMetadata;

		this.relationalValueSource = new ImplicitDiscriminatorColumnSource( entityTypeMetadata );
	}

	@Override
	public RelationalValueSource getDiscriminatorRelationalValueSource() {
		return relationalValueSource;
	}

	@Override
	public String getExplicitHibernateTypeName() {
		return StandardBasicTypes.STRING.getName();
	}

	@Override
	public EntityNaming getEntityNaming() {
		return entityTypeMetadata;
	}

	@Override
	public MetadataBuildingContext getBuildingContext() {
		return entityTypeMetadata.getLocalBindingContext();
	}
}
