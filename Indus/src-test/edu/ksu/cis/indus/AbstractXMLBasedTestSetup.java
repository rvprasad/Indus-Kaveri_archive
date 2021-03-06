
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

package edu.ksu.cis.indus;

import edu.ksu.cis.indus.common.soot.IStmtGraphFactory;

import edu.ksu.cis.indus.xmlizer.IJimpleIDGenerator;

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
	 * ID generator used while xmlizing documents which will often be the case (xmlize and test the xmlized data).
	 */
	protected IJimpleIDGenerator idGenerator;

	/** 
	 * This is the location where the jimple xml should be dumpled.
	 */
	protected String dumpLocation;

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
	 * Creates an instance of this class.
	 *
	 * @param test to be enclosed by this setup.
	 *
	 * @pre test != null
	 *
	 * @see TestSetup#TestSetup(Test)
	 */
	public AbstractXMLBasedTestSetup(final TestSuite test) {
		super(test);
	}

	/**
	 * Sets the location where jimple xml should be dumped.
	 *
	 * @param location to dump jimple xml.
	 *
	 * @pre location != null
	 */
	public final void setJimpleXMLDumpLocation(final String location) {
		dumpLocation = location;
	}

	/**
	 * @see edu.ksu.cis.indus.IXMLBasedTest#setIdGenerator(edu.ksu.cis.indus.xmlizer.IJimpleIDGenerator)
	 */
	public void setIdGenerator(final IJimpleIDGenerator generator) {
		idGenerator = generator;
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
	 * @return the cfg factory
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
	 * Retrieves the directory from which one of the xml-based testing input is read from.
	 *
	 * @return directory in which one of the xml-based testing input is read from.
	 */
	public String getXMLControlDir() {
		return xmlFirstInputDir;
	}

	/**
	 * @see IXMLBasedTest#setXMLTestDir(String)
	 */
	public void setXMLTestDir(final String xmlInDir) {
		xmlSecondInputDir = xmlInDir;
	}

	/**
	 * Retrieves the directory from which the other xml-based testing input is read from.
	 *
	 * @return directory in which the other xml-based testing input is read from.
	 */
	public String getXMLTestDir() {
		return xmlSecondInputDir;
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
			_tester.setIdGenerator(idGenerator);
		}
	}

	/**
	 * @see junit.extensions.TestSetup#tearDown()
	 */
	protected void tearDown()
	  throws Exception {
		if (stmtGraphFactory != null) {
			stmtGraphFactory.reset();
			stmtGraphFactory = null;
		}

		if (idGenerator != null) {
			idGenerator.reset();
			idGenerator = null;
		}
		System.gc();
	}
}

// End of File
