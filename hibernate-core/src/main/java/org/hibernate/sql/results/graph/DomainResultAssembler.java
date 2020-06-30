/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Responsible for "assembling" a result for inclusion in the domain query
 * result.  "Assembling" the result basically means building the result object
 * (whatever that means for a specific result type) and returning it for
 * injection into the result "row" currently being processed
 *
 * @author Steve Ebersole
 */
public interface DomainResultAssembler<J> {
	// todo (6.0) : rename GraphNodeAssembler (?)
	// todo (6.0) : move (and related) to new `org.hibernate.sql.results.assemble` package (?)

	/**
	 * The main "assembly" contract.  Assemble the result and return it.
	 */
	J assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options);

	/**
	 * Convenience form of {@link #assemble(RowProcessingState, JdbcValuesSourceProcessingOptions)}
	 */
	default J assemble(RowProcessingState rowProcessingState) {
		return assemble( rowProcessingState, rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions() );
	}

	/**
	 * The JavaTypeDescriptor describing the Java type that this assembler
	 * assembles.
	 */
	JavaTypeDescriptor<J> getAssembledJavaTypeDescriptor();
}
