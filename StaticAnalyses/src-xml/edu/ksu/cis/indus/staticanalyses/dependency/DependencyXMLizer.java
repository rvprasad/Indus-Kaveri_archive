
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

import edu.ksu.cis.indus.common.soot.IStmtGraphFactory;

import edu.ksu.cis.indus.interfaces.ICallGraphInfo;
import edu.ksu.cis.indus.interfaces.IEnvironment;

import edu.ksu.cis.indus.processing.IProcessor;
import edu.ksu.cis.indus.processing.ProcessingController;

import edu.ksu.cis.indus.staticanalyses.flow.instances.ofa.processors.CGBasedXMLizingProcessingFilter;

import edu.ksu.cis.indus.xmlizer.AbstractXMLizer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This class provides the logic to xmlize dependence information.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public final class DependencyXMLizer
  extends AbstractXMLizer {
	/**
	 * This is used to identify statement level dependence producing analysis.
	 */
	public static final Object STMT_LEVEL_DEPENDENCY;

	/**
	 * This maps dependency ids to dependence sort ids (STMT_LEVEL_DEPENDENCY).
	 */
	protected static final Properties PROPERTIES;

	static {
		STMT_LEVEL_DEPENDENCY = "STMT_LEVEL_DEPENDENCY";
		PROPERTIES = new Properties();

		String _propFileName = System.getProperty("indus.dependencyxmlizer.properties.file");

		if (_propFileName == null) {
			_propFileName = "edu/ksu/cis/indus/staticanalyses/dependency/DependencyXMLizer.properties";
		}

		final InputStream _stream = ClassLoader.getSystemResourceAsStream(_propFileName);

		try {
			PROPERTIES.load(_stream);
		} catch (IOException _e) {
			System.err.println("Well, error loading property file.  Bailing.");
			throw new RuntimeException(_e);
		}
	}

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Log LOGGER = LogFactory.getLog(DependencyXMLizer.class);

	/**
	 * The name of the files into which dependence information was written into.
	 */
	final Collection filenames = new ArrayList();

	/**
	 * @see edu.ksu.cis.indus.xmlizer.AbstractXMLizer#getFileName(java.lang.String)
	 */
	public String getFileName(final String name) {
		return "dependence_" + xmlizeString(name) + ".xml";
	}

	/**
	 * Flushes the writes associated with each xmlizers.
	 *
	 * @param xmlizers to be flushed.
	 * @param ctrl to unhook the xmlizers from.
	 *
	 * @pre xmlizers != null and ctrl != null
	 */
	public void flushXMLizers(final Map xmlizers, final ProcessingController ctrl) {
		for (final Iterator _i = xmlizers.keySet().iterator(); _i.hasNext();) {
			final IProcessor _p = (IProcessor) _i.next();
			_p.unhook(ctrl);

			try {
				final FileWriter _f = (FileWriter) xmlizers.get(_p);
				_f.flush();
				_f.close();
			} catch (IOException _e) {
				_e.printStackTrace();
				LOGGER.error("Failed to close the xml file based on " + _p.getClass(), _e);
			}
		}
	}

	/**
	 * @see edu.ksu.cis.indus.xmlizer.AbstractXMLizer#writeXML(java.util.Map)
	 */
	public void writeXML(final Map info) {
		final ProcessingController _ctrl = new ProcessingController();
		_ctrl.setEnvironment((IEnvironment) info.get(IEnvironment.ID));
		_ctrl.setStmtGraphFactory((IStmtGraphFactory) info.get(IStmtGraphFactory.ID));
		_ctrl.setProcessingFilter(new CGBasedXMLizingProcessingFilter((ICallGraphInfo) info.get(ICallGraphInfo.ID)));

		final Map _xmlizers = initXMLizers(info, _ctrl);
		_ctrl.process();
		flushXMLizers(_xmlizers, _ctrl);
	}

	/**
	 * Retrieves the part of the filename based on the given analysis.
	 *
	 * @param da to be used to base the name.
	 *
	 * @return the derived name.
	 *
	 * @pre da != null
	 * @post result != null
	 */
	String getDAPartOfFileName(final DependencyAnalysis da) {
		return da.getId() + ":" + da.getClass().getName();
	}

	/**
	 * Initializes the xmlizers.
	 *
	 * @param info is the name of the root method.
	 * @param ctrl is the controller to be used to initialize the xmlizers and to which to hook up the xmlizers to xmlize the
	 * 		  dependence information.
	 *
	 * @return a map of xmlizers and the associated writers.
	 *
	 * @throws IllegalStateException when output directory is unspecified.
	 *
	 * @pre rootname != null and ctrl != null
	 * @post result != null and result.oclIsKindOf(Map(StmtLevelDependencyXMLizer, Writer))
	 */
	private Map initXMLizers(final Map info, final ProcessingController ctrl) {
		final Map _result = new HashMap();

		if (getXmlOutputDir() == null) {
			LOGGER.fatal("Defaulting to current directory for xml output.");
			throw new IllegalStateException("Please specify an output directory while using the xmlizer.");
		}

		for (final Iterator _i = DependencyAnalysis.ids.iterator(); _i.hasNext();) {
			final Object _id = _i.next();
			final Collection _col = (Collection) info.get(_id);

			if (_col != null) {
				for (final Iterator _j = _col.iterator(); _j.hasNext();) {
					final DependencyAnalysis _da = (DependencyAnalysis) _j.next();
					String _providedFileName = (String) info.get(FILE_NAME_ID);

					if (_providedFileName == null) {
						_providedFileName = getDAPartOfFileName(_da);
					}

					final String _filename = getFileName(_providedFileName);
					filenames.add(_filename);

					final File _f = new File(getXmlOutputDir() + File.separator + _filename);

					try {
						final FileWriter _writer = new FileWriter(_f);
						final StmtLevelDependencyXMLizer _xmlizer = getXMLizerFor(_writer, _da);

						if (_xmlizer == null) {
							LOGGER.error("No xmlizer specified for dependency calculated by " + _da.getClass()
								+ ".  No xml file written.");
							_writer.close();
						} else {
							_xmlizer.hookup(ctrl);
							_result.put(_xmlizer, _writer);
						}
					} catch (IOException _e) {
						LOGGER.error("Failed to write the xml file based on " + _da.getClass(), _e);
					}
				}
			}
		}
		return _result;
	}

	/**
	 * Retrives the xmlizer for the given dependence analysis based on the properties.
	 *
	 * @param writer to be used by the xmlizer.
	 * @param da is the dependence analysis for which the xmlizer is requested.
	 *
	 * @return the xmlizer.
	 *
	 * @pre writer != null and da != null
	 * @post result != null
	 */
	private StmtLevelDependencyXMLizer getXMLizerFor(final Writer writer, final DependencyAnalysis da) {
		StmtLevelDependencyXMLizer _result = null;
		final String _xmlizerId = da.getId().toString();

		final String _temp = PROPERTIES.getProperty(_xmlizerId);

		if (_temp.equals(DependencyXMLizer.STMT_LEVEL_DEPENDENCY)) {
			_result = new StmtLevelDependencyXMLizer(writer, getIdGenerator(), da);
		} else {
			LOGGER.error("Unknown dependency xmlizer type requested.  Bailing on this.");
		}
		return _result;
	}
}

/*
   ChangeLog:
   $Log$
   Revision 1.15  2004/04/25 21:18:37  venku
   - refactoring.
     - created new classes from previously embedded classes.
     - xmlized jimple is fragmented at class level to ease comparison.
     - id generation is embedded into the testing framework.
     - many more tiny stuff.

   Revision 1.14  2004/04/18 08:58:58  venku
   - enabled test support for slicer.
   Revision 1.13  2004/03/29 09:32:25  venku
   - documentation.
   - formatting.
   Revision 1.12  2004/03/29 09:31:01  venku
   - adds .xml to the retrieved filename.
   - always defaults to a non-empty DA based file name.
   Revision 1.11  2004/03/29 01:55:03  venku
   - refactoring.
     - history sensitive work list processing is a common pattern.  This
       has been captured in HistoryAwareXXXXWorkBag classes.
   - We rely on views of CFGs to process the body of the method.  Hence, it is
     required to use a particular view CFG consistently.  This requirement resulted
     in a large change.
   - ripple effect of the above changes.
   Revision 1.10  2004/03/09 18:40:03  venku
   - refactoring.
   - moved methods common to XMLBased Test into AbstractXMLBasedTest.
   Revision 1.9  2004/03/05 11:59:45  venku
   - documentation.
   Revision 1.8  2004/02/25 23:34:29  venku
   - classes that should not be visible should be invisible :-)
   Revision 1.7  2004/02/09 17:40:53  venku
   - dependence and call graph info serialization is done both ways.
   - refactored the xmlization framework.
     - Each information type has a xmlizer (XMLizer)
     - Each information type has a xmlizer driver (XMLizerCLI)
     - Tests use the XMLizer.
   Revision 1.6  2004/02/09 07:46:37  venku
   - added new class to xmlize OFA info.
   Revision 1.5  2004/02/09 06:49:02  venku
   - deleted dependency xmlization and test classes.
 */
