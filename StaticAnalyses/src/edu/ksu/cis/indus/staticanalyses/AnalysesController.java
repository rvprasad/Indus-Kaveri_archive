
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

package edu.ksu.cis.indus.staticanalyses;

import soot.SootMethod;

import soot.toolkits.graph.CompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;

import edu.ksu.cis.indus.interfaces.ISystemInfo;
import edu.ksu.cis.indus.staticanalyses.dependency.DependencyAnalysis;
import edu.ksu.cis.indus.staticanalyses.interfaces.AbstractAnalysis;
import edu.ksu.cis.indus.staticanalyses.interfaces.IProcessor;
import edu.ksu.cis.indus.staticanalyses.processing.ProcessingController;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * This class is provides the control class for the analyses suite. The analyses progress in phases. It may be so that some
 * application require a particular sequence in which each analysis should progress. Hence, the applications provide an
 * implementation of controller interface to drive the analyses in a particular sequence of phases.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$
 */
public class AnalysesController
  implements ISystemInfo {
	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Log LOGGER = LogFactory.getLog(AnalysesController.class);

	/**
	 * A map from methods(<code>SootMethod</code>) to their complete statement graph(<code>CompleteStmtGraph</code>).
	 *
	 * @invariant method2cmpltstmtGraph != null
	 */
	protected final Map method2cmpltStmtGraph;

	/**
	 * The map of analysis being controlled by this object. It maps names of analysis to the analysis object.
	 *
	 * @invariant participatingAnalyses != null
	 * @invariant participatingAnalyses.oclIsTypeOf(Map(Object, DependencyAnalysis)
	 */
	protected final Map participatingAnalyses;

	/**
	 * This is the preprocessing controlling agent.
	 *
	 * @invariant preprocessController != null;
	 */
	protected final ProcessingController preprocessController;

	/**
	 * The status of this controller and it's controllees.  The information provided by it's controllees is valid only when
	 * this field is <code>true</code>.
	 */
	protected boolean stable = false;

	/**
	 * This is a map of name to objects which provide information that maybe used by analyses, but is of no use to the
	 * controller.
	 */
	private Map info;

	/**
	 * Creates a new AbstractAnalysesController object.
	 *
	 * @param infoPrm is a map of name to objects which provide information that maybe used by analyses, but is of no use to
	 * 		  the controller.
	 * @param pc is the preprocessing controller.
	 *
	 * @pre pc != null;
	 */
	public AnalysesController(final Map infoPrm, final ProcessingController pc) {
		participatingAnalyses = new HashMap();
		method2cmpltStmtGraph = new HashMap();
		this.info = infoPrm;
		this.preprocessController = pc;
	}

	/**
	 * Sets the implementation to be used for an analysis.
	 *
	 * @param id of the analysis.
	 * @param analysis is the implementation of the named analysis.
	 *
	 * @pre id != null and analysis != null
	 */
	public final void setAnalysis(final Object id, final AbstractAnalysis analysis) {
		participatingAnalyses.put(id, analysis);

		if (analysis.doesPreProcessing()) {
			IProcessor p = analysis.getPreProcessor();
			p.hookup(preprocessController);
		}
	}

	/**
	 * Provides the implementation registered for the given analysis purpose.
	 *
	 * @param id of the requested analysis.  This has to be one of the names(XXX_DA) defined in this class.
	 *
	 * @return the implementation registered for the given purpose.  <code>null</code>, if there is no registered analysis.
	 */
	public final AbstractAnalysis getAnalysis(final Object id) {
		AbstractAnalysis result = null;

		if (participatingAnalyses != null) {
			result = (AbstractAnalysis) participatingAnalyses.get(id);
		}
		return result;
	}

	/**
	 * Provides the statement graph for the given method.
	 *
	 * @param method for which the statement graph is requested.
	 *
	 * @return the statement graph.  <code>null</code> is returned if the method was not processed.
	 *
	 * @pre method != null
	 * @post method2cmpltStmtGraph.contains(method) == false implies result = null
	 */
	public UnitGraph getStmtGraph(final SootMethod method) {
		return (UnitGraph) method2cmpltStmtGraph.get(method);
	}

	/**
	 * Executes the analyses in the registered order.
	 */
	public void execute() {
		boolean analyzing;
		Collection done = new ArrayList();

		do {
			analyzing = false;

			for (Iterator i = participatingAnalyses.keySet().iterator(); i.hasNext();) {
				String daName = (String) i.next();
				AbstractAnalysis temp = (AbstractAnalysis) participatingAnalyses.get(daName);

				if (temp != null && !done.contains(temp)) {
					temp.analyze();

					boolean t = temp.isStable();

					if (t) {
						done.add(temp);
					}
					analyzing |= t;
				}
			}
		} while (analyzing);
		stable = true;
	}

	/**
	 * Initializes the controller.  The data structures dependent on <code>methods</code> are initialized, the interested
	 * analyses are asked to preprocess the data, and then the analyses are initialized.
	 *
	 * @param methods that form the system to be analyzed.
	 *
	 * @pre methods != null
	 */
	public void initialize(final Collection methods) {
		stable = false;

		Collection failed = new ArrayList();

		preprocessController.process();

		for (Iterator i = methods.iterator(); i.hasNext();) {
			SootMethod method = (SootMethod) i.next();
			CompleteUnitGraph sg = new CompleteUnitGraph(method.retrieveActiveBody());
			method2cmpltStmtGraph.put(method, sg);
		}

		for (Iterator k = participatingAnalyses.keySet().iterator(); k.hasNext();) {
			Object key = k.next();
			AbstractAnalysis da = (AbstractAnalysis) participatingAnalyses.get(key);

			try {
				da.initialize(method2cmpltStmtGraph, info);
			} catch (InitializationException e) {
				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn(da.getClass() + " failed to initialize, hence, it will not executed.", e);
				}
				failed.add(key);
			}
		}

		for (Iterator i = failed.iterator(); i.hasNext();) {
			participatingAnalyses.remove(i.next());
		}
	}

	/**
	 * Resets the internal data structures of the controller.  This resets the participating analyses.  This does not reset
	 * the Object Flow AbstractAnalysis instance.
	 */
	public void reset() {
		for (Iterator i = participatingAnalyses.values().iterator(); i.hasNext();) {
			AbstractAnalysis element = (DependencyAnalysis) i.next();
			element.reset();
		}
		participatingAnalyses.clear();
		method2cmpltStmtGraph.clear();
	}
}

/*
   ChangeLog:
   $Log$
   Revision 1.18  2003/09/12 22:33:08  venku
   - AbstractAnalysis extends IStatus.  Hence, analysis() does not return a value.
   - Ripple effect of the above changes.
   Revision 1.17  2003/09/12 01:22:17  venku
   - coding conventions.
   Revision 1.16  2003/09/09 00:44:33  venku
   - removed unnecessay field.
   Revision 1.15  2003/08/25 08:51:45  venku
   Coding convention and Formatting.
   Revision 1.14  2003/08/25 08:40:47  venku
   Formatting.
   Revision 1.13  2003/08/25 08:39:58  venku
   Well, it does not make sense to specify a set of IDs and expect only
   analyses of these IDs to be controlled.  This is more like application
   logic than framework logic.
   Revision 1.12  2003/08/25 08:06:39  venku
   Renamed participatingAnalysesNames to participatingAnalysesIDs.
   AbstractAnalysesController now has a method to extract the above field.
   Revision 1.11  2003/08/25 07:28:01  venku
   Ripple effect of renaming AbstractController to AbstractAnalysesController.
   Revision 1.10  2003/08/18 04:44:35  venku
   Established an interface which will provide the information about the underlying system as required by transformations.
   It is called ISystemInfo.
   Ripple effect of the above change.
   Revision 1.9  2003/08/18 04:10:10  venku
   Documentation change.
   Revision 1.8  2003/08/18 04:08:22  venku
   Removed unnecessary method.
   Revision 1.7  2003/08/18 00:59:50  venku
   Changed specification to fit the last change.
   Revision 1.6  2003/08/18 00:59:11  venku
   Changed the type of the IDs to java.lang.Object to provide extensibility.
   Ripple effect of that happens in AbstractController.
   Revision 1.5  2003/08/16 02:41:37  venku
   Renamed AController to AbstractController.
   Renamed AAnalysis to AbstractAnalysis.
   Revision 1.4  2003/08/15 08:23:09  venku
   Renamed getDAnalysis to getAnalysis.
   Revision 1.3  2003/08/11 08:49:34  venku
   Javadoc documentation errors were fixed.
   Some classes were documented.
   Revision 1.2  2003/08/11 07:46:09  venku
   Finalized the parameters.
   Spruced up Documentation and Specification.
   Revision 1.1  2003/08/07 06:42:16  venku
   Major:
    - Moved the package under indus umbrella.
    - Renamed isEmpty() to hasWork() in WorkBag.
 */
