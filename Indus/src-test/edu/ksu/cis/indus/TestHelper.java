
/*
 * Indus, a toolkit to customize and adapt Java programs.
 * Copyright (c) 2003, 2004, 2005 SAnToS Laboratory, Kansas State University
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

package edu.ksu.cis.indus;

import edu.ksu.cis.indus.common.datastructures.HistoryAwareFIFOWorkBag;
import edu.ksu.cis.indus.common.datastructures.IWorkBag;

import java.io.File;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * This is a class with helper methods that can be used in conjunction with JUnit.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public final class TestHelper {
	///CLOVER:OFF

	/**
	 * Creates a new TestHelper object.
	 */
	private TestHelper() {
	}

	///CLOVER:ON

	/**
	 * Retrieves all instances of <code>TestCase</code> of type <code>type</code> reachable from the given test suite.
	 *
	 * @param suite is the test suite to be drilled.
	 * @param type of instance of the <code>TestCase</code> the user is interested in.
	 *
	 * @return a collection of <code>TestCase</code> objects of type <code>type</code>.
	 *
	 * @pre suite != null and type != null
	 * @post result != null
	 * @post result->forall(o | o.oclIsKindOf(junit.framework.TestCase) and type.isInstance(o))
	 */
	public static Collection getTestCasesReachableFromSuite(final TestSuite suite, final Class type) {
		final Collection _result = new HashSet();
		final IWorkBag _workList = new HistoryAwareFIFOWorkBag(new HashSet());
		_workList.addAllWorkNoDuplicates(Collections.list(suite.tests()));

		while (_workList.hasWork()) {
			final Test _o = (Test) _workList.getWork();

			if (type.isInstance(_o)) {
				_result.add(_o);
			} else if (_o instanceof TestSuite) {
				_workList.addAllWorkNoDuplicates(Collections.list(((TestSuite) _o).tests()));
			}
		}
		return _result;
	}

	/**
	 * Appends the containing suite's name to the tests.
	 *
	 * @param suite containing the tests whose name should be altered.
	 * @param recursive if all reachable test case's names should be altered.
	 *
	 * @pre suite != null
	 */
	public static void appendSuiteNameToTestsIn(final TestSuite suite, final boolean recursive) {
		final String _suiteName = suite.getName();

		for (final Enumeration _e = suite.tests(); _e.hasMoreElements();) {
			final Test _test = (Test) _e.nextElement();

			if (_test instanceof IndusTestCase) {
				final IndusTestCase _t = (IndusTestCase) _test;

				if (_suiteName != null) {
					_t.setTestName(suite.getName() + ":" + _t.getName());
				}
			} else if (_test instanceof IXMLBasedTest) {
				final IXMLBasedTest _t = (IXMLBasedTest) _test;

				if (_suiteName != null) {
					((AbstractXMLBasedTest) _t).setTestName(suite.getName() + ":" + ((AbstractXMLBasedTest) _t).getName());
				}
			} else if (_test instanceof TestSuite) {
				final TestSuite _t = (TestSuite) _test;

				if (_suiteName != null) {
					_t.setName(suite.getName() + ":" + _t.getName());
				}

				if (recursive) {
					appendSuiteNameToTestsIn((TestSuite) _test, recursive);
				}
			}
		}
	}

	/**
	 * Checks if the given xml test based on given test and control directory should be executed.  It should be if the
	 * directories exists and they are readable/writable suitably.
	 *
	 * @param configuration is the name of the test configuration.
	 * @param xmlTestDir is the directory in which to find the test data.
	 * @param xmlControlDir is the directory in which to find the text control data.
	 *
	 * @return a string of zero length if the test should be executed;  a string of non-zero length, if the test should not
	 * 		   be executed.  In the latter case, the string contains the message why the test should not be executed.
	 *
	 * @pre xmlTestDir != null and xmlControlDir != null
	 * @post result != null
	 */
	public static String checkXMLBasedTestExecutability(final String configuration, final String xmlTestDir,
		final String xmlControlDir) {
		final StringBuffer _sb = new StringBuffer();
		File _f = new File(xmlControlDir);

		if (!_f.exists() || !_f.canRead()) {
			_sb.append("Control directory " + xmlControlDir + " does not exists. Bailing on " + configuration);
		}
		_f = new File(xmlTestDir);

		if (!_f.exists() || !_f.canWrite()) {
			_sb.append("Test directory " + xmlTestDir + " does not exists. Bailing on " + configuration);
		}
		return _sb.toString();
	}
}

// End of File
