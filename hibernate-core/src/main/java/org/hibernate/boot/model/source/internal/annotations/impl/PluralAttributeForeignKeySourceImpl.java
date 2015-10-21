/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.jandex.spi.JpaDotNames;
import org.hibernate.boot.model.source.internal.annotations.ForeignKeyContributingSource;
import org.hibernate.boot.model.source.internal.annotations.PluralAttributeForeignKeySource;
import org.hibernate.boot.model.source.internal.annotations.RelationalValueSource;
import org.hibernate.boot.model.source.internal.annotations.metadata.attribute.Column;
import org.hibernate.boot.model.source.internal.annotations.metadata.attribute.PluralAttribute;
import org.hibernate.boot.model.source.internal.annotations.util.AnnotationBindingHelper;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;

/**
 * @author Hardy Ferentschik
 * @author Strong Liu <stliu@hibernate.org>
 */
public class PluralAttributeForeignKeySourceImpl implements PluralAttributeForeignKeySource {
	private final PluralAttribute attribute;
	private final ForeignKeyInformation foreignKeyInformation;
	private final boolean isCascadeDeleteEnabled;
	private final JoinColumnResolutionDelegateImpl joinColumnResolutionDelegate;

	public PluralAttributeForeignKeySourceImpl(PluralAttribute attribute) {
		this.attribute = attribute;
		this.foreignKeyInformation = ForeignKeyInformation.from(
				AnnotationBindingHelper.findFirstNonNull(
						attribute.findAnnotation( JpaDotNames.JOIN_TABLE )
				),
				attribute.getContext()
		);
		this.isCascadeDeleteEnabled = attribute.getOnDeleteAction() == OnDeleteAction.CASCADE;
		this.joinColumnResolutionDelegate = new JoinColumnResolutionDelegateImpl( attribute );
	}
	@Override
	public boolean isCascadeDeleteEnabled() {
		return isCascadeDeleteEnabled;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return true;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return true;
	}

	@Override
	public List<RelationalValueSource> getRelationalValueSources() {
		List<Column> joinClumnValues = attribute.getJoinColumnValues();
		if ( joinClumnValues.isEmpty() ) {
			return Collections.emptyList();
		}
		List<RelationalValueSource> result = new ArrayList<RelationalValueSource>( joinClumnValues.size() );
		for ( Column joinColumn : joinClumnValues ) {
			result.add( new ColumnSourceImpl( joinColumn ) );
		}
		return result;
	}

	@Override
	public ForeignKeyInformation getForeignKeyInformation() {
		return foreignKeyInformation;
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		return joinColumnResolutionDelegate;
	}

	public static class JoinColumnResolutionDelegateImpl implements ForeignKeyContributingSource.JoinColumnResolutionDelegate {
		private final PluralAttribute attribute;

		public JoinColumnResolutionDelegateImpl(PluralAttribute attribute) {
			this.attribute = attribute;
		}

		@Override
		public List<? extends Selectable> getJoinColumns(ForeignKeyContributingSource.JoinColumnResolutionContext context) {
			List<Column> joinColumnValues = attribute.getJoinColumnValues();
			if ( joinColumnValues.isEmpty() ) {
				return null;
			}
			List<Selectable> result = new ArrayList<Selectable>( joinColumnValues.size() );
			for ( Column column : attribute.getJoinColumnValues() ) {
				result.add( context.resolveColumn( column.getReferencedColumnName(), null, null, null ) );
			}
			return result;
		}

		@Override
		public String getReferencedAttributeName() {
			return null;
		}

		@Override
		public Table getReferencedTable(ForeignKeyContributingSource.JoinColumnResolutionContext context) {
			return context.resolveTableForAttribute( null );
		}
	}
}
