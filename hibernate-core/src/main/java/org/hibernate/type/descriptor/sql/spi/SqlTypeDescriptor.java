/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

import org.hibernate.annotations.Remove;
import org.hibernate.sql.JdbcValueBinder;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.JdbcValueMapper;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Describes a JDBC/SQL type.
 *
 * @author Steve Ebersole
 */
public interface SqlTypeDescriptor extends org.hibernate.type.descriptor.sql.SqlTypeDescriptor {
	/**
	 * Is this descriptor available for remapping?
	 * <p/>
	 * Mainly this comes into play as part of Dialect SqlTypeDescriptor remapping,
	 * which is how we handle LOB binding e.g. But some types should not allow themselves
	 * to be remapped.
	 *
	 * @return {@code true} indicates this descriptor can be remapped; otherwise, {@code false}
	 *
	 * @see WrapperOptions#remapSqlTypeDescriptor
	 * @see org.hibernate.dialect.Dialect#remapSqlTypeDescriptor
	 * @see org.hibernate.dialect.Dialect#getSqlTypeDescriptorOverride
	 */
	boolean canBeRemapped();

	/**
	 * Get the JavaTypeDescriptor for the Java type recommended by the JDBC spec for mapping the
	 * given JDBC/SQL type.  The standard implementations honor the JDBC recommended mapping as per
	 * http://docs.oracle.com/javase/1.5.0/docs/guide/jdbc/getstart/mapping.html
	 *
	 * @param typeConfiguration Access to Hibernate's current TypeConfiguration (type information)
	 *
	 * @return the recommended Java type descriptor.
	 */
	<T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration);

	<T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor);

	<T> JdbcValueMapper getJdbcValueMapper(BasicJavaDescriptor<T> javaTypeDescriptor);

	// todo (6.0) : have getBinder and getExtractor return

	/**
	 * Get the binder (setting JDBC in-going parameter values) capable of handling values of the type described by the
	 * passed descriptor.
	 *
	 * @param javaTypeDescriptor The descriptor describing the types of Java values to be bound
	 *
	 * @return The appropriate binder.
	 */
	@Remove
	default <X> JdbcValueBinder<X> getBinder(BasicJavaDescriptor<X> javaTypeDescriptor) {
		return getJdbcValueMapper( javaTypeDescriptor ).getJdbcValueBinder();
	}

	/**
	 * Get the extractor (pulling out-going values from JDBC objects) capable of handling values of the type described
	 * by the passed descriptor.
	 *
	 * @param javaTypeDescriptor The descriptor describing the types of Java values to be extracted
	 *
	 * @return The appropriate extractor
	 */
	@Remove
	default <X> JdbcValueExtractor<X> getExtractor(BasicJavaDescriptor<X> javaTypeDescriptor) {
		return getJdbcValueMapper( javaTypeDescriptor ).getJdbcValueExtractor();
	}

}
