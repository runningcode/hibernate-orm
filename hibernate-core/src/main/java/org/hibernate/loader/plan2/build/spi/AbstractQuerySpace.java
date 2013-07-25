/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan2.build.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.spi.AbstractPlanNode;
import org.hibernate.loader.plan2.build.internal.spaces.QuerySpacesImpl;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.loader.plan2.spi.QuerySpace;

/**
 * Convenience base class for QuerySpace implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractQuerySpace extends AbstractPlanNode implements QuerySpace {
	private final String uid;
	private final Disposition disposition;
	private final QuerySpacesImpl querySpaces;

	private List<Join> joins;

	public AbstractQuerySpace(
			String uid,
			Disposition disposition,
			QuerySpacesImpl querySpaces,
			SessionFactoryImplementor sessionFactory) {
		super( sessionFactory );
		this.uid = uid;
		this.disposition = disposition;
		this.querySpaces = querySpaces;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return super.sessionFactory();
	}

	// todo : copy ctor - that depends how graph copying works here...


	/**
	 * Provides subclasses access to the spaces to which this space belongs.
	 *
	 * @return The query spaces
	 */
	public QuerySpacesImpl getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public String getUid() {
		return uid;
	}

	@Override
	public Disposition getDisposition() {
		return disposition;
	}

	@Override
	public Iterable<Join> getJoins() {
		return joins == null
				? Collections.<Join>emptyList()
				: joins;
	}

	protected List<Join> internalGetJoins() {
		if ( joins == null ) {
			joins = new ArrayList<Join>();
		}

		return joins;
	}
}
