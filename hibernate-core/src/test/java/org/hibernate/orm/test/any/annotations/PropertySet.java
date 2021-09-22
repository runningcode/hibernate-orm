/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.any.annotations;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MetaValue;

@Entity
@Table( name = "property_set" )
public class PropertySet {
	private Integer id;
	private String name;
	private Property someProperty;

	private List<Property> generalProperties = new ArrayList<Property>();

	public PropertySet() {
		super();
	}

	public PropertySet(String name) {
		this.name = name;
	}

	@ManyToAny(
			metaColumn = @Column( name = "property_type" ) )
	@AnyMetaDef( idType = "integer", metaType = "string",
			metaValues = {
			@MetaValue( value = "S", targetEntity = StringProperty.class ),
			@MetaValue( value = "I", targetEntity = IntegerProperty.class ) } )
	@Cascade( { CascadeType.ALL } )
	@JoinTable( name = "obj_properties", joinColumns = @JoinColumn( name = "obj_id" ),
			inverseJoinColumns = @JoinColumn( name = "property_id" ) )
	public List<Property> getGeneralProperties() {
		return generalProperties;
	}

	public void setGeneralProperties(List<Property> generalProperties) {
		this.generalProperties = generalProperties;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Any( metaColumn = @Column( name = "property_type" ) )
	@Cascade( value = { CascadeType.ALL } )
	@AnyMetaDef( idType = "integer", metaType = "string", metaValues = {
	@MetaValue( value = "S", targetEntity = StringProperty.class ),
	@MetaValue( value = "I", targetEntity = IntegerProperty.class )
			} )
	@JoinColumn( name = "property_id" )
	public Property getSomeProperty() {
		return someProperty;
	}

	public void setSomeProperty(Property someProperty) {
		this.someProperty = someProperty;
	}

	public void addGeneralProperty(Property property) {
		this.generalProperties.add( property );
	}
}