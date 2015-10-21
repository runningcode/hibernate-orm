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
package org.hibernate.boot.model.source.internal.annotations.metadata.attribute;

import org.hibernate.boot.model.source.spi.PluralAttributeElementNature;

import org.jboss.jandex.ClassInfo;

/**
 * Presents metadata about the elements of a plural attribute
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeElementDetails {
	/**
	 * Get the descriptor for the Java type of the collection elements.
	 *
	 * @return The descriptor for the Java type of the collection elements.
	 */
	ClassInfo getJavaType();

	/**
	 * Get the nature of the element values.
	 *
	 * @return The element nature
	 */
	PluralAttributeElementNature getElementNature();
}
