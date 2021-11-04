/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.contribution.array;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class StringArrayJdbcType implements JdbcType {
	@Override
	public int getJdbcTypeCode() {
		return Types.ARRAY;
	}

	@Override
	public <T> BasicJavaType<T> getJdbcRecommendedJavaTypeMapping(Integer precision, Integer scale, TypeConfiguration typeConfiguration) {
		return (BasicJavaType<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( String[].class );
	}

	private final ValueBinder<String[]> binder = new BasicBinder<String[]>( StringArrayJavaType.INSTANCE, this ) {
		@Override
		protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
			// according to documentation, can be treated as character data
			st.setNull( index, Types.ARRAY );
		}

		@Override
		protected void doBind(PreparedStatement st, String[] value, int index, WrapperOptions options) throws SQLException {
			st.setObject( index,value );
		}

		@Override
		protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
			// according to documentation, can be treated as character data
			st.setNull( name, Types.ARRAY );
		}

		@Override
		protected void doBind(CallableStatement st, String[] value, String name, WrapperOptions options) throws SQLException {
			st.setObject( name, value );
		}
	};

	private final ValueExtractor<String[]> extractor = new BasicExtractor<String[]>( StringArrayJavaType.INSTANCE, this ) {
		@Override
		protected String[] doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
			return (String[]) rs.getObject( paramIndex );
		}

		@Override
		protected String[] doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
			return (String[]) statement.getObject( index );
		}

		@Override
		protected String[] doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
			return (String[]) statement.getObject( name );
		}
	};

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaTypeDescriptor) {
		//noinspection unchecked
		return (ValueBinder<X>) binder;
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaTypeDescriptor) {
		//noinspection unchecked
		return (ValueExtractor<X>) extractor;
	}
}
