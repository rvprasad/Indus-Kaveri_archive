
/*
 * Indus, a toolkit to customize and adapt Java programs.
 * Copyright (c) 2003 SAnToS Laboratory, Kansas State University
 *
 * This software is licensed under the KSU Open Academic License.
 * You should have received a copy of the license with the distribution.
 * A copy can be found at
 *     http://www.cis.ksu.edu/santos/license.html
 * or you can contact the lab at:
 *     SAnToS Laboratory
 *     234 Nichols Hall
 *     Manhattan, KS 66506, USA
 */

package edu.ksu.cis.indus.common.datastructures;

import java.util.HashSet;


/**
 * This class tests <code>HistoryAwareLIFOWorkBag</code> class.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class HistoryAwareLIFOWorkBagTest
  extends HistoryAwareAbstractWorkBagTest {
	/**
	 * Tests <code>addWork</code> method.
	 */
	public final void testAddWork() {
		final Object _o1 = "test1";
		final Object _o2 = "test1";
		wb.addWork(_o1);
		wb.addWork(_o2);
		assertTrue(wb.getWork() == _o1);
		assertFalse(wb.hasWork());
	}

	/**
	 * @see TestCase#setUp()
	 */
	protected void setUp()
	  throws Exception {
		wb = new HistoryAwareLIFOWorkBag(new HashSet());
	}

	/**
	 * @see TestCase#tearDown()
	 */
	protected void tearDown()
	  throws Exception {
		wb = null;
	}
}

/*
   ChangeLog:
   $Log$
 */
