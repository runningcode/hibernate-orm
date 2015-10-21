/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.annotations;

import org.hibernate.boot.model.source.spi.ToolingHint;
import org.hibernate.boot.model.source.spi.ToolingHintContext;

/**
 * @author Steve Ebersole
 */
public class ToolingHintContextExtended extends ToolingHintContext {
	/**
	 * Singleton access
	 */
	public static final ToolingHintContextExtended INSTANCE = new ToolingHintContextExtended();

	public ToolingHintContextExtended() {
		super( null );
	}

	@Override
	public void add(ToolingHint toolingHint) {
		// no-op
	}
}
