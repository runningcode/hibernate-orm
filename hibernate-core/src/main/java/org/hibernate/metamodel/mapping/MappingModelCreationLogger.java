/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * Logger used during mapping-model creation
 *
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90005701, max = 90005800 )
public interface MappingModelCreationLogger extends BasicLogger {
	String LOGGER_NAME = "org.hibernate.orm.model.mapping.creation";

	MappingModelCreationLogger LOGGER = Logger.getMessageLogger( MappingModelCreationLogger.class, LOGGER_NAME );

	static String subLoggerName(String subName) {
		return LOGGER_NAME + "." + subName;
	}

	static Logger subLogger(String subName) {
		return Logger.getLogger( subLoggerName( subName ) );
	}

	boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
	boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();
}
