
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

package edu.ksu.cis.indus;

import edu.ksu.cis.indus.common.soot.IStmtGraphFactory;

import java.util.Collection;
import java.util.Iterator;

import junit.extensions.TestSetup;

import junit.framework.TestSuite;


/**
 * This class is a test setup class that is used in conjunction with xml data-based testing.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class AbstractXMLBasedTestSetup
  extends TestSetup
  implements IXMLBasedTest {
	/**
	 * The statement graph (CFG) factory used during testing.
	 */
	private IStmtGraphFactory stmtGraphFactory;

	/**
	 * The directory in which one of the xml-based testing input is read from.
	 */
	private String xmlFirstInputDir;

	/**
	 * The directory in which the other xml-based testing input is read from.
	 */
	private String xmlSecondInputDir;

	/**
	 * @see TestSetup#TestSetup(TestSuite)
	 */
	public AbstractXMLBasedTestSetup(final TestSuite test) {
		super(test);
	}

	/**
	 * Retrieves the directory from which one of the xml-based testing input is read from.
	 *
	 * @return directory in which one of the xml-based testing input is read from.
	 */
	public String getFirstXmlInputDir() {
		return xmlFirstInputDir;
	}

	/**
	 * Retrieves the directory from which the other xml-based testing input is read from.
	 *
	 * @return directory in which the other xml-based testing input is read from.
	 */
	public String getSecondXmlInputDir() {
		return xmlSecondInputDir;
	}

	/**
	 * Sets the CFG factory to be used during testing.
	 *
	 * @param cfgFactory is the factory to be used.
	 */
	public void setStmtGraphFactory(final IStmtGraphFactory cfgFactory) {
		stmtGraphFactory = cfgFactory;
	}

	/**
	 * Retrieves the CFG factory used during testing.
	 *
	 * @return DOCUMENT ME!
	 */
	public IStmtGraphFactory getStmtGraphFactory() {
		return stmtGraphFactory;
	}

	/**
	 * @see IXMLBasedTest#setXMLControlDir(String)
	 */
	public void setXMLControlDir(final String xmlInDir) {
		xmlFirstInputDir = xmlInDir;
	}

	/**
	 * @see IXMLBasedTest#setXMLTestDir(String)
	 */
	public void setXMLTestDir(final String xmlInDir) {
		xmlSecondInputDir = xmlInDir;
	}

	/**
	 * @see junit.extensions.TestSetup#setUp()
	 */
	protected void setUp()
	  throws Exception {
		final Collection _temp = TestHelper.getTestCasesReachableFromSuite((TestSuite) getTest(), IXMLBasedTest.class);

		for (final Iterator _i = _temp.iterator(); _i.hasNext();) {
			final IXMLBasedTest _tester = (IXMLBasedTest) _i.next();
			_tester.setXMLTestDir(xmlSecondInputDir);
			_tester.setXMLControlDir(xmlFirstInputDir);
			_tester.setStmtGraphFactory(stmtGraphFactory);
		}
	}

	/**
	 * @see junit.extensions.TestSetup#tearDown()
	 */
	protected void tearDown()
	  throws Exception {
		System.gc();
	}
}

/*
   ChangeLog:
   $Log$
   Revision 1.7  2004/04/17 22:07:37  venku
   - changed the names of firstInputDir/secondInputDir to testDir/controlDir.
   - ripple effect in interfaces, classes, and property files.
   Revision 1.6  2004/03/29 01:55:16  venku
   - refactoring.
     - history sensitive work list processing is a common pattern.  This
       has been captured in HistoryAwareXXXXWorkBag classes.
   - We rely on views of CFGs to process the body of the method.  Hence, it is
     required to use a particular view CFG consistently.  This requirement resulted
     in a large change.
   - ripple effect of the above changes.
   Revision 1.5  2004/03/05 11:59:40  venku
   - documentation.
   Revision 1.4  2004/02/14 23:16:49  venku
   - coding convention.
   Revision 1.3  2004/02/09 04:39:40  venku
   - refactoring test classes still..
   - need to make xmlizer classes independent of their purpose.
     Hence, they need to be highly configurable.
   - For each concept, test setup should be in TestSetup
     rather than in the XMLizer.
   Revision 1.2  2004/02/09 01:20:06  venku
   - coding convention.
   - added a new abstract class contain the logic required for xml-based
     testing.  (AbstractXMLBasedTest)
   - added a new xml-based call graph testing class.
   Revision 1.1  2004/02/08 04:53:14  venku
   - refactoring!!!
   - All regression tests implement IXMLBasedTest.
   - All test setups extends AbstractXMLBasedTestSetup.
   - coding convention.
   - all tests occur at the same package as the classes
     being tested.
 */
