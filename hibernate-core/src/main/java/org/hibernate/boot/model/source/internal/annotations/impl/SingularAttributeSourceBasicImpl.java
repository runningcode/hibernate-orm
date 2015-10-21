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


import java.util.ArrayList;
import java.util.List;
import javax.persistence.AttributeConverter;

import org.hibernate.boot.internal.AttributeConverterDescriptorNonAutoApplicableImpl;
import org.hibernate.boot.model.source.internal.annotations.RelationalValueSource;
import org.hibernate.boot.model.source.internal.annotations.RelationalValueSourceContainer;
import org.hibernate.boot.model.source.internal.annotations.SingularAttributeSourceBasic;
import org.hibernate.boot.model.source.internal.annotations.metadata.attribute.AttributeOverride;
import org.hibernate.boot.model.source.internal.annotations.metadata.attribute.BasicAttribute;
import org.hibernate.boot.model.source.internal.annotations.metadata.attribute.Column;
import org.hibernate.boot.model.source.internal.annotations.metadata.attribute.OverrideAndConverterCollector;
import org.hibernate.boot.model.source.internal.annotations.metadata.type.EntityBindingContext;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.SingularAttributeNature;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.AttributeConverterDescriptor;
import org.hibernate.tuple.GenerationTiming;

/**
 * @author Steve Ebersole
 */
public class SingularAttributeSourceBasicImpl
		extends SingularAttributeSourceImpl
		implements SingularAttributeSourceBasic, RelationalValueSourceContainer {
	private final List<RelationalValueSource> relationalValueSources;

	public SingularAttributeSourceBasicImpl(
			BasicAttribute attribute,
			OverrideAndConverterCollector overrideAndConverterCollector) {
		super( attribute );

		final AttributeOverride override = overrideAndConverterCollector.locateAttributeOverride(
				attribute.getPath()
		);
		validateAttributeOverride( override );

		this.relationalValueSources = buildRelationalValueSources( attribute, override );
	}

	private List<RelationalValueSource> buildRelationalValueSources(
			BasicAttribute attribute,
			AttributeOverride attributeOverride) {
		final List<RelationalValueSource> relationalValueSources = new ArrayList<RelationalValueSource>();
		if ( attribute.getFormulaValue() != null ) {
			relationalValueSources.add( new DerivedValueSourceImpl( attribute.getFormulaValue() ) );
		}
		else {
			final int explicitColumnCount = attribute.getColumnValues().size();

			if ( explicitColumnCount == 0 ) {
				Column overrideColumn = attributeOverride == null ? null : attributeOverride.getImpliedColumn();
				if ( overrideColumn != null
						|| attribute.getCustomReadFragment() != null
						|| attribute.getCustomWriteFragment() != null
						|| attribute.getCheckCondition() != null ) {
					relationalValueSources.add(
							new ColumnSourceImpl(
									overrideColumn,
									null,
									attribute.getCustomReadFragment(),
									attribute.getCustomWriteFragment(),
									attribute.getCheckCondition()
							)
					);
				}
			}
			else if ( explicitColumnCount == 1 ) {
				Column column = attribute.getColumnValues().get( 0 );
				if ( attributeOverride != null ) {
					column.applyColumnValues( attributeOverride.getOverriddenColumnInfo() );
				}
				relationalValueSources.add(
						new ColumnSourceImpl(
								column,
								null,
								attribute.getCustomReadFragment(),
								attribute.getCustomWriteFragment(),
								attribute.getCheckCondition()
						)
				);
			}
			else {
				if ( attributeOverride != null ) {
					throw attribute.getContainer().getLocalBindingContext().makeMappingException(
							"Cannot apply AttributeOverride to attribute mapped to more than one column : "
									+ attribute.getBackingMember().toString()
					);
				}

				for ( Column column : attribute.getColumnValues() ) {
					relationalValueSources.add( new ColumnSourceImpl( column, null ) );
				}
			}
		}

		return relationalValueSources;
	}

	protected void validateAttributeOverride(AttributeOverride override) {
	}

	@Override
	public BasicAttribute getAnnotatedAttribute() {
		return (BasicAttribute) super.getAnnotatedAttribute();
	}

	@Override
	public SingularAttributeNature getSingularAttributeNature() {
		return SingularAttributeNature.BASIC;
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return null;
	}

	@Override
	public Boolean isInsertable() {
		return getAnnotatedAttribute().isInsertable();
	}

	@Override
	public Boolean isUpdatable() {
		return getAnnotatedAttribute().isUpdatable();
	}

	@Override
	public boolean isBytecodeLazy() {
		return getAnnotatedAttribute().isLazy();
	}

	@Override
	public AttributePath getAttributePath() {
		return getAnnotatedAttribute().getPath();
	}

	@Override
	public boolean isCollectionElement() {
		return false;
	}

	@Override
	public AttributeRole getAttributeRole() {
		return getAnnotatedAttribute().getRole();
	}

	@Override
	public List<RelationalValueSource> getRelationalValueSources() {
		return relationalValueSources;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return false;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return false;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return false;
	}

	@Override
	public EntityBindingContext getBuildingContext() {
		return getAnnotatedAttribute().getContext();
	}

	@Override
	public AttributeConverterDescriptor resolveAttributeConverterDescriptor() {
		if ( relationalValueSources.size() > 1 ) {
			return null;
		}

		// First look for @Converts...
		final ConvertConversionInfo conversionInfo = getAnnotatedAttribute().getContainer()
				.locateConversionInfo( getAttributePath() );
		if ( conversionInfo != null ) {
			if ( !conversionInfo.isConversionEnabled() ) {
				return null;
			}

			final ClassLoaderService cls = getBuildingContext().getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );
			try {
				final Class<? extends AttributeConverter> converterClass = cls.classForName( conversionInfo.getConverterTypeDescriptor().name().toString() );
				return new AttributeConverterDescriptorNonAutoApplicableImpl( converterClass.newInstance() );
			}
			catch (Exception e) {
				throw getBuildingContext().makeMappingException( "Could not create AttributeConverter for attribute : " + getName(), e );
			}
		}

		// And then fallback to
		return getBuildingContext().getMetadataCollector()
				.getAttributeConverterAutoApplyHandler()
				.findAutoApplyConverterForAttribute( getAnnotatedAttribute().getBackingMember(), getBuildingContext() );
	}
}
