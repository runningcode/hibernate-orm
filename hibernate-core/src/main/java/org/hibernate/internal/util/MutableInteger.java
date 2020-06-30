/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util;

/**
 * A more performant version of {@link java.util.concurrent.atomic.AtomicInteger} in cases
 * where we do not have to worry about concurrency.  So usually as a variable referenced in
 * anonymous-inner or lambda or ...
 *
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class MutableInteger {
	private int value;

	public MutableInteger() {
		this( 0 );
	}

	public MutableInteger(int value) {
		this.value = value;
	}

	public MutableInteger deepCopy() {
		return new MutableInteger( value );
	}

	public int getAndIncrement() {
		return value++;
	}

	public int incrementAndGet() {
		return ++value;
	}

	public int get() {
		return value;
	}

	public void set(int value) {
		this.value = value;
	}

	public void increment() {
		++value;
	}

	public void decrement() {
		--value;
	}
}
