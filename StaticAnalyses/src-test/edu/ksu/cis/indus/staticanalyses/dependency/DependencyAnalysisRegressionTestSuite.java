
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

package edu.ksu.cis.indus.staticanalyses.dependency;

import edu.ksu.cis.indus.ErringTestCase;
import edu.ksu.cis.indus.IXMLBasedTest;
import edu.ksu.cis.indus.TestHelper;

import edu.ksu.cis.indus.common.soot.ExceptionFlowSensitiveStmtGraphFactory;
import edu.ksu.cis.indus.common.soot.IStmtGraphFactory;

import edu.ksu.cis.indus.staticanalyses.flow.FATest;
import edu.ksu.cis.indus.staticanalyses.flow.instances.ofa.XMLBasedOFATest;
import edu.ksu.cis.indus.staticanalyses.flow.instances.ofa.processors.CallGraphTest;
import edu.ksu.cis.indus.staticanalyses.flow.instances.ofa.processors.XMLBasedCallGraphTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import junit.textui.TestRunner;


/**
 * This is the test suite used to run dependency analyses related tests using JUnit's text interface to the runner.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class DependencyAnalysisRegressionTestSuite
  extends TestCase {
	/**
	 * This is the property via which the ofa test accepts input.  Refer to DepedencyAnalysisTest.properties for format.
	 */
	public static final String DEPENDENCY_ANALYSIS_TEST_PROPERTIES_FILE =
		"indus.staticanalyses.dependency.DependencyAnalysisTest.properties.file";

	///CLOVER:OFF

	/**
	 * This is the entry point via command-line.
	 *
	 * @param args are ignored.
	 */
	public static void main(final String[] args) {
		final String[] _suiteName = { DependencyAnalysisRegressionTestSuite.class.getName() };
		TestRunner.main(_suiteName);
	}

	///CLOVER:ON

	/**
	 * Provides the suite of tests in junit-style.  This sets up the tests based on the file specified via
	 * <code>DEPENDENCY_ANALYSIS_TEST_PROPERTIES_FILE</code> system property.  Refer to
	 * <code>edu.ksu.cis.indus.staticanalyses.dependency.DependencyAnalysisTest.properties</code> for the format of the
	 * file.
	 *
	 * @return the suite of tests.
	 *
	 * @throws RuntimeException when <code>DEPENDENCY_ANALYSIS_TEST_PROPERTIES_FILE</code> property is unspecified.
	 */
	public static Test suite() {
		final TestSuite _suite = new TestSuite("Test for edu.ksu.cis.indus.staticanalyses.dependency");
		final String _propFileName = System.getProperty(DEPENDENCY_ANALYSIS_TEST_PROPERTIES_FILE);

		if (_propFileName == null) {
			throw new RuntimeException("Please provide a property file like DependencyAnalysisTest.properties via " + "-D"
				+ DEPENDENCY_ANALYSIS_TEST_PROPERTIES_FILE);
		}
		setupTests(_propFileName, _suite);
		return _suite;
	}

	/**
	 * Sets up the test fixture.
	 *
	 * @param propFileName is the name of the file with the data to setup the test fixture.
	 * @param suite will contain new tests based on the fixture data (upon return).
	 *
	 * @throws IllegalArgumentException when the fixture data is invalid.
	 *
	 * @pre propFileName != null and suite != null
	 */
	private static void setupTests(final String propFileName, final TestSuite suite) {
		final Properties _props = new Properties();

		try {
			_props.load(new FileInputStream(new File(propFileName)));

			final String[] _configs = _props.getProperty("configs").split(" ");
			final Collection _das = new ArrayList();
			final IStmtGraphFactory _stmtGraphFactory =
				new ExceptionFlowSensitiveStmtGraphFactory(ExceptionFlowSensitiveStmtGraphFactory.SYNC_RELATED_EXCEPTIONS,
					true);

			for (int _i = 0; _i < _configs.length; _i++) {
				final String _config = _configs[_i];
				final String _classNames = _props.getProperty(_config + ".classNames");
				final String _xmlTestDir = _props.getProperty(_config + IXMLBasedTest.XML_TEST_DIR_PROP_SUFFIX);
				final String _xmlControlDir = _props.getProperty(_config + IXMLBasedTest.XML_CONTROL_DIR_PROP_SUFFIX);
				final String _classpath = _props.getProperty(_config + ".classpath");
                final String _jimpleXMLDumpDir = _props.getProperty(_config + ".jimpleXMLDumpDirectory");
				final String _str = TestHelper.checkXMLBasedTestExecutability(_config, _xmlTestDir, _xmlControlDir);

				try {
					final TestSuite _temp = new TestSuite(_config);

					if (_str.length() > 0) {
						_temp.addTest(new ErringTestCase(_str));
						TestHelper.appendSuiteNameToTestsIn(_temp, true);
						suite.addTest(_temp);
					} else {
						_das.clear();
						_das.add(new DivergenceDA());
						_das.add(new EntryControlDA());
						_das.add(new ExitControlDA());
						_das.add(new IdentifierBasedDataDA());
						_das.add(new InterferenceDAv1());
						_das.add(new InterferenceDAv2());
						_das.add(new InterferenceDAv3());
						_das.add(new ReadyDAv1());
						_das.add(new ReadyDAv2());
						_das.add(new ReadyDAv3());
						_das.add(new ReferenceBasedDataDA());
						_das.add(new SynchronizationDA());

						String _ignoreDARegex = _props.getProperty("ignore.das");

						if (_ignoreDARegex == null) {
							_ignoreDARegex = "^$";
						}

						final DependencyXMLizer _xmlizer = new DependencyXMLizer();

						for (final Iterator _j = _das.iterator(); _j.hasNext();) {
							final Object _da = _j.next();

							if (!Pattern.matches(_ignoreDARegex, _da.getClass().getName())) {
								final XMLBasedDependencyAnalysisTest _test =
									new XMLBasedDependencyAnalysisTest((DependencyAnalysis) _da, _xmlizer);
								_temp.addTest(_test);
							}
						}
						_temp.addTestSuite(XMLBasedCallGraphTest.class);
						_temp.addTestSuite(CallGraphTest.class);
						_temp.addTestSuite(XMLBasedOFATest.class);
						_temp.addTestSuite(FATest.class);
						TestHelper.appendSuiteNameToTestsIn(_temp, true);

						final DependencyAnalysisTestSetup _test =
							new DependencyAnalysisTestSetup(_temp, _classNames, _classpath);
                        _test.setJimpleXMLDumpLocation(_jimpleXMLDumpDir);
						_test.setStmtGraphFactory(_stmtGraphFactory);
						_test.setXMLTestDir(_xmlTestDir);
						_test.setXMLControlDir(_xmlControlDir);
						suite.addTest(_test);
					}
				} catch (IllegalArgumentException _e) {
					;
				}
			}
		} catch (final IOException _e) {
			throw new IllegalArgumentException("Specified property file does not exist.");
		}
	}
}

/*
   ChangeLog:
   $Log$
   Revision 1.6  2004/04/21 04:13:20  venku
   - jimple dumping takes time.  Instead, the user can control this
     per configuration.

   Revision 1.5  2004/04/20 06:53:17  venku
   - documentation.
   Revision 1.4  2004/04/20 05:27:14  venku
   - renamed checkExecutability() to checkXMLBasedTestExecutability().
   Revision 1.3  2004/04/20 00:40:34  venku
   - coding conventions.
   Revision 1.2  2004/04/18 00:23:38  venku
   - coding conventions.
   Revision 1.1  2004/04/18 00:22:37  venku
   - DependencyAnalysisRegresssionTestSuite was renamed to
     DependenceAnalysisRegressionTestSuite.  One missing "s".
   Revision 1.9  2004/04/18 00:17:20  venku
   - added support to dump jimple.xml while testing. (bug fix)
   Revision 1.8  2004/04/17 23:35:42  venku
   - failures due to unavailable resources were not flagged. FIXED
     - added a new class which always errs.
     - this new class is used to setup a test case for cases where an error should occur.
     - ripple effect.
   Revision 1.7  2004/04/17 22:07:34  venku
   - changed the names of firstInputDir/secondInputDir to testDir/controlDir.
   - ripple effect in interfaces, classes, and property files.
   Revision 1.6  2004/04/05 23:16:33  venku
   - textui.TestRunner cannot be run via start(). FIXED.
   Revision 1.5  2004/04/05 22:26:48  venku
   - used textui.TestRunner instead of swingui.TestRunner.
   Revision 1.4  2004/04/01 22:33:49  venku
   - test suite name was incorrect.
   Revision 1.3  2004/04/01 19:18:29  venku
   - stmtGraphFactory was not set.
   Revision 1.2  2004/03/29 09:44:41  venku
   - finished the xml-based testing framework for dependence.
   Revision 1.1  2004/03/09 18:40:03  venku
   - refactoring.
   - moved methods common to XMLBased Test into AbstractXMLBasedTest.
 */
