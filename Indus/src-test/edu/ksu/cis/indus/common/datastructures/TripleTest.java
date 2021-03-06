
/*******************************************************************************
 * Indus, a program analysis and transformation toolkit for Java.
 * Copyright (c) 2001, 2007 Venkatesh Prasad Ranganath
 * 
 * All rights reserved.  This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 which accompanies 
 * the distribution containing this program, and is available at 
 * http://www.opensource.org/licenses/eclipse-1.0.php.
 * 
 * For questions about the license, copyright, and software, contact 
 * 	Venkatesh Prasad Ranganath at venkateshprasad.ranganath@gmail.com
 *                                 
 * This software was developed by Venkatesh Prasad Ranganath in SAnToS Laboratory 
 * at Kansas State University.
 *******************************************************************************/

package edu.ksu.cis.indus.common.datastructures;

import edu.ksu.cis.indus.IndusTestCase;

import java.util.ArrayList;
import java.util.Collection;


/**
 * This class tests <code>TripleTest</code>.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class TripleTest
  extends IndusTestCase {
	/** 
	 * A triple.
	 */
	private Triple triple1;

	/** 
	 * A triple.
	 */
	private Triple triple2;

	/**
	 * Tests <code>getFirst</code>.
	 */
	public final void testGetFirst() {
		assertTrue(triple1.getFirst().equals("first"));
	}

	/**
	 * Tests <code>getSecond</code>.
	 */
	public final void testGetSecond() {
		assertTrue(triple1.getSecond().equals("second"));
	}

	/**
	 * Tests <code>getThird</code>.
	 */
	public final void testGetThird() {
		assertTrue(triple1.getThird().equals("third"));
	}

	/**
	 * Tests <code>hashCode</code> and <code>equals</code>.
	 */
	public final void testHashCodeAndEquals() {
		final Triple _t3 = new Triple("first", "second", "third");
		assertTrue(triple1.hashCode() != triple2.hashCode());
		assertTrue(triple1.hashCode() == _t3.hashCode());
		assertFalse(triple1.equals(triple2));
		assertTrue(triple1.equals(_t3));

		final Triple _t4 = new Triple(null, null, null);
		final Triple _t5 = new Triple(null, null, null);
		assertTrue(_t4.equals(_t5));
		assertTrue(_t4.hashCode() == _t5.hashCode());
		assertFalse(_t4.equals("hi"));
	}

	/**
	 * Tests <code>optimize</code> and <code>unoptimize</code>.
	 */
	public final void testOptimizeAndUnOptimize1() {
		final Collection _second = new ArrayList();
		_second.add("first");

		final Triple _t1 = new Triple("first", _second, "third");
		_t1.optimize();

		final int _hash1 = _t1.hashCode();
		_second.add("second");

		assertTrue(_t1.hashCode() == _hash1);
		_t1.unoptimize();
		assertTrue(_t1.hashCode() != _hash1);
		_t1.optimize();
		assertTrue(_t1.hashCode() != _hash1);
	}

	/**
	 * Tests <code>optimize</code> and <code>unoptimize</code>.
	 */
	public final void testOptimizeAndUnOptimize2() {
		final StringBuffer _second = new StringBuffer();
		_second.append("first");

		final Triple _t1 = new Triple("first", _second, "third");
		final int _hash1 = _t1.hashCode();
		_second.append("second");

		assertTrue(_t1.hashCode() == _hash1);
		_t1.unoptimize();
		assertTrue(_t1.hashCode() == _hash1);
		_t1.optimize();
		assertTrue(_t1.hashCode() == _hash1);
	}

	/**
	 * Tests <code>toString</code>.
	 */
	public final void testToString() {
		final StringBuffer _second = new StringBuffer();
		_second.append(true);

		final Triple _t1 = new Triple("first", _second, "third");
		_t1.optimize();

		final String _str1 = _t1.toString();
		_t1.unoptimize();
		_second.append(false);

		final String _str2 = _t1.toString();
		assertFalse(_str1.equals(_str2));
		_t1.optimize();

		final String _str3 = _t1.toString();
		assertFalse(_str1.equals(_str3));
		assertTrue(_str2.equals(_str3));
	}

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp()
	  throws Exception {
		triple1 = new Triple("first", "second", "third");
		triple2 = new Triple("fourth", "fifth", "sixth");
	}

	/**
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown()
	  throws Exception {
		triple1 = null;
		triple2 = null;
	}
}

// End of File
