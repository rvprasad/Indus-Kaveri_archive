
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

import soot.SootMethod;

import edu.ksu.cis.indus.staticanalyses.InitializationException;
import edu.ksu.cis.indus.staticanalyses.interfaces.ICallGraphInfo;
import edu.ksu.cis.indus.staticanalyses.support.BasicBlockGraph;
import edu.ksu.cis.indus.staticanalyses.support.BasicBlockGraph.BasicBlock;
import edu.ksu.cis.indus.staticanalyses.support.DirectedGraph;
import edu.ksu.cis.indus.staticanalyses.support.FIFOWorkBag;
import edu.ksu.cis.indus.staticanalyses.support.IWorkBag;
import edu.ksu.cis.indus.staticanalyses.support.Pair;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * This class provides intraprocedural control dependency information. This implementation refers to the technical report <a
 * href="http://www.cis.ksu.edu/santos/papers/technicalReports.html">A Formal  Study of Slicing for Multi-threaded Program
 * with JVM Concurrency Primitives"</a>.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$
 *
 * @invariant dependeeMap.oclIsKindOf(Map(SootMethod, Sequence(Stmt)))
 * @invariant dependeeMap.entrySet()->forall(o | o.getValue().size() = o.getKey().getActiveBody().getUnits().size())
 * @invariant dependentMap.oclIsKindOf(Map(SootMethod, Sequence(Set(Stmt))))
 * @invariant dependentMap.entrySet()->forall(o | o.getValue().size() = o.getKey().getActiveBody().getUnits().size())
 */
public class ControlDA
  extends DependencyAnalysis {
	/*
	 * The dependence information is stored as follows: For each method, a list of collection is maintained.  Each location in
	 * the list corresponds to the statement at the same location in the statement list of the method.  The collection is the
	 * statements to which the statement at the location of the collection is related via control dependence.
	 */

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Log LOGGER = LogFactory.getLog(ControlDA.class);

	/**
	 * This indicates the dependence information is required in the forward direction assuming all heads to the CFG as entry
	 * points.
	 */
	public static final Object FORWARD = "starting from the heads";

	/**
	 * This indicates the dependence information is required in the backward direction, i.e. forward direction on the
	 * reversed form of the CFG.
	 */
	public static final Object BACKWARD = "starting from the tails";

	/**
	 * This captures the direction of the information calculated by this object.  <code>true</code> indicates that the head
	 * into the graph is considered as entry points into the CFG.  <code>false</code>  indicates that the tails of the graph
	 * should be considered as entry points into the CFG.
	 */
	private final boolean forward;

	/**
	 * This provides the call graph information.
	 */
	private ICallGraphInfo callgraph;

	/**
	 * Creates a new ControlDA object.
	 *
	 * @param direction of the information.
	 *
	 * @throws IllegalArgumentException when the direction is not one of <code>BACKWARD</code> or <code>FORWARD</code>.
	 */
	public ControlDA(final Object direction) {
		if (direction.equals(BACKWARD)) {
			forward = false;
		} else if (direction.equals(FORWARD)) {
			forward = true;
		} else {
			throw new IllegalArgumentException(
				"theDirection argument should be DependencyAnalysis.FORWARD or DependencyAnalysis.BACKWARD.");
		}
	}

	/**
	 * Returns the statements on which <code>dependentStmt</code> depends on in the given <code>method</code>.
	 *
	 * @param dependentStmt is the dependent of interest.
	 * @param method in which <code>dependentStmt</code> occurs.
	 *
	 * @return a collection of statements.
	 *
	 * @pre dependentStmt.oclIsKindOf(Stmt)
	 * @pre method.oclIsTypeOf(SootMethod)
	 * @post result->forall(o | o.isOclKindOf(Stmt)) and result.size() == 1
	 *
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.DependencyAnalysis#getDependees(java.lang.Object, java.lang.Object)
	 */
	public Collection getDependees(final Object dependentStmt, final Object method) {
		Collection result = Collections.EMPTY_LIST;
		List list = (List) dependeeMap.get(method);

		if (list != null) {
			int index = getStmtList((SootMethod) method).indexOf(dependentStmt);

			if (list.get(index) != null) {
				result = Collections.unmodifiableCollection((Collection) list.get(index));
			}
		}
		return result;
	}

	/**
	 * Returns the statements which depend on <code>dependeeStmt</code> in the given <code>method</code>.
	 *
	 * @param dependeeStmt is the dependee of interest.
	 * @param method in which <code>dependentStmt</code> occurs.
	 *
	 * @return a collection of statements.
	 *
	 * @pre dependeeStmt.isOclKindOf(Stmt)
	 * @pre method.oclIsTypeOf(SootMethod)
	 * @post result->forall(o | o.isOclKindOf(Stmt))
	 *
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.DependencyAnalysis#getDependents(java.lang.Object, java.lang.Object)
	 */
	public Collection getDependents(final Object dependeeStmt, final Object method) {
		Collection result = Collections.EMPTY_LIST;
		List list = (List) dependentMap.get(method);

		if (list != null) {
			int index = getStmtList((SootMethod) method).indexOf(dependeeStmt);

			if (list.get(index) != null) {
				result = Collections.unmodifiableCollection((Collection) list.get(index));
			}
		}
		return result;
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.DependencyAnalysis#getId()
	 */
	public Object getId() {
		return DependencyAnalysis.CONTROL_DA;
	}

	/**
	 * Calculates the control dependency information for the methods provided during initialization.
	 *
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.DependencyAnalysis#analyze()
	 */
	public void analyze() {
		stable = false;

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("BEGIN: Control Dependence processing");
		}

		for (Iterator i = callgraph.getReachableMethods().iterator(); i.hasNext();) {
			SootMethod currMethod = (SootMethod) i.next();
			BasicBlockGraph bbGraph = getBasicBlockGraph(currMethod);

			if (bbGraph == null) {
				LOGGER.error("Method " + currMethod.getSignature() + " did not have a basic block graph.");
				continue;
			}

			BitSet[] bbCDBitSets = computeControlDependency(bbGraph);
			fixupMaps(bbGraph, bbCDBitSets, currMethod);
		}

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("END: Control Dependence processing");
		}
		stable = true;
	}

	/**
	 * Returns a stringized representation of this analysis.  The representation includes the results of the analysis.
	 *
	 * @return a stringized representation of this object.
	 */
	public String toString() {
		StringBuffer result =
			new StringBuffer("Statistics for control dependence as calculated by " + getClass().getName() + "\n");
		int localEdgeCount = 0;
		int edgeCount = 0;

		StringBuffer temp = new StringBuffer();

		for (Iterator i = dependeeMap.entrySet().iterator(); i.hasNext();) {
			Map.Entry entry = (Map.Entry) i.next();
			SootMethod method = (SootMethod) entry.getKey();
			localEdgeCount = 0;

			List stmts = getStmtList(method);
			List cd = (List) entry.getValue();

			for (int j = 0; j < stmts.size(); j++) {
				if (cd == null) {
					continue;
				}

				Collection dees = (Collection) cd.get(j);

				if (dees != null) {
					temp.append("\t\t" + stmts.get(j) + " --> " + dees + "\n");
					localEdgeCount += dees.size();
				} else {
					temp.append("\t\t" + stmts.get(j) + " --> METHOD_ENTRY\n");
				}
			}

			result.append("\tFor " + entry.getKey() + " there are " + localEdgeCount + " control dependence edges.\n");
			result.append(temp);
			temp.delete(0, temp.length());
			edgeCount += localEdgeCount;
		}
		result.append("A total of " + edgeCount + " control dependence edges exist.");
		return result.toString();
	}

	/**
	 * Sets up internal data structures.
	 *
	 * @throws InitializationException when call graph service is not provided.
	 *
	 * @pre info.get(ICallGraphInfo.ID) != null and info.get(ICallGraphInfo.ID).oclIsTypeOf(ICallGraphInfo)
	 *
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.AbstractAnalysis#setup()
	 */
	protected void setup()
	  throws InitializationException {
		super.setup();
		callgraph = (ICallGraphInfo) info.get(ICallGraphInfo.ID);

		if (callgraph == null) {
			throw new InitializationException(ICallGraphInfo.ID + " was not provided.");
		}
	}

	/**
	 * Calculates the control dependency from a directed graph.  This calculates the dependence information in terms of nodes
	 * in the graph.  This is later translated to statement level information by {@link #fixupMaps fixupMaps}.
	 *
	 * @param graph for which dependence info needs to be calculated.  Each node in the graph should have an unique index and
	 * 		  the indices should start from 0.
	 *
	 * @return an array of bitsets.  The length of the array and each of the bitset in it is equal to the number of nodes in
	 * 		   the graph.  The nth bitset captures the dependence information via set bits.  The BitSets capture
	 * 		   dependent->dependee information.
	 *
	 * @post result.oclIsTypeOf(Sequence(BitSet)) and result->size() == graph.getNodes().size()
	 * @post result->forall(o | o.size() == graph.getNodes().size())
	 */
	private BitSet[] computeControlDependency(final DirectedGraph graph) {
		Map dag = graph.getDAG();
		final List NODES = graph.getNodes();
		final int NUM_OF_NODES = NODES.size();
		int[] succsSize = new int[NUM_OF_NODES];
		BitSet[][] cd = new BitSet[NUM_OF_NODES][NUM_OF_NODES];
		BitSet[] result = new BitSet[NUM_OF_NODES];
		Collection processed = new ArrayList();
		BitSet currResult = new BitSet();
		BitSet temp1 = new BitSet();
		IWorkBag wb = new FIFOWorkBag();
		Collection roots;

		if (forward) {
			roots = graph.getHeads();
		} else {
			roots = graph.getTails();
		}
		wb.addAllWorkNoDuplicates(roots);

		while (wb.hasWork()) {
			BasicBlock bb = (BasicBlock) wb.getWork();
			Pair dagBlock = (Pair) dag.get(bb);
			Collection preds;

			if (forward) {
				preds = (Collection) dagBlock.getFirst();
			} else {
				preds = (Collection) dagBlock.getSecond();
			}

			if (!processed.containsAll(preds)) {
				wb.addWorkNoDuplicates(bb);
				continue;
			}

			// propogate data to the successors   
			int currIndex = NODES.indexOf(bb);
			Collection succs;

			if (forward) {
				succs = (Collection) dagBlock.getSecond();
			} else {
				succs = (Collection) dagBlock.getFirst();
			}

			BitSet[] currCD = cd[currIndex];
			succsSize[currIndex] = succs.size();

			for (Iterator j = processed.iterator(); j.hasNext();) {
				int pIndex = NODES.indexOf(j.next());
				BitSet pCD = currCD[pIndex];

				if (pCD != null) {
					boolean assignFlag = pCD.cardinality() == succsSize[pIndex];

					if (!assignFlag) {
						currResult.set(pIndex);
					}

					for (Iterator i = succs.iterator(); i.hasNext();) {
						int succIndex = NODES.indexOf(i.next());
						BitSet[] succCDs = cd[succIndex];

						if (assignFlag) {
							succCDs[pIndex] = pCD;
						} else {
							BitSet succCD = succCDs[pIndex];

							if (succCD == null) {
								succCD = new BitSet();
								succCDs[pIndex] = succCD;
							}
							succCD.or(pCD);
						}
					}
				}
			}

			if (succsSize[currIndex] > 1) {
				int count = 0;

				for (Iterator i = succs.iterator(); i.hasNext();) {
					int succIndex = NODES.indexOf(i.next());
					BitSet succCD = cd[succIndex][currIndex];

					if (succCD == null) {
						succCD = new BitSet();
						cd[succIndex][currIndex] = succCD;
					}

					succCD.set(count++);
				}
			}

			if (!currResult.isEmpty()) {
				if (currResult.length() > 1) {
					// prune the dom set to a mere control-dom set.
					temp1.clear();

					for (Iterator i = preds.iterator(); i.hasNext();) {
						BitSet t = result[NODES.indexOf(i.next())];

						if (t != null) {
							temp1.and(t);
						}
					}

					for (int j = currResult.nextSetBit(0); j >= 0; j = currResult.nextSetBit(j + 1)) {
						if (!preds.contains(NODES.get(j)) && (preds.size() == 1 || !temp1.get(j))) {
							currResult.clear(j);
						}
					}
				}
			} else {
				// add dependence to the reachable roots
				for (Iterator i = roots.iterator(); i.hasNext();) {
					BasicBlock root = (BasicBlock) i.next();

					if (graph.isReachable(bb, root, !forward)) {
						currResult.set(NODES.indexOf(root));
					}
				}
			}
			result[currIndex] = currResult;
			currResult = new BitSet();

			// Add the successors of the node 
			wb.addAllWorkNoDuplicates(succs);
			processed.add(bb);
		}

		return result;
	}

	/**
	 * Translates the dependence information as captured in <code>bbCDBitSets</code> to statement level info and populates
	 * the dependeXXMap fields.
	 *
	 * @param graph is the basic block graph corresponding to <code>method</code>.
	 * @param bbCDBitSets is the array that contains the basic block level dependence information as calculated by {@link
	 * 		  #computeControlDependency(DirectedGraph) computeControlDependency}.
	 * @param method for which the maps are being populated.
	 *
	 * @pre graph != null and bbCDBitSets != null and method != null
	 * @post dependentMap.get(method) != null
	 * @post dependentMap.values()->forall(o | o->forall(p | p != null()))
	 * @post dependeeMap.get(method) != null
	 * @post dependeeMap.values()->forall(o | o->forall(p | p != null()))
	 */
	private void fixupMaps(final BasicBlockGraph graph, final BitSet[] bbCDBitSets, final SootMethod method) {
		List nodes = graph.getNodes();
		List sl = getStmtList(method);
		List mDependee = new ArrayList();
		List mDependent = new ArrayList();

		for (int i = sl.size(); i > 0; i--) {
			mDependee.add(null);
			mDependent.add(null);
		}

		boolean flag = false;

		for (int i = bbCDBitSets.length - 1; i >= 0; i--) {
			BitSet cd = bbCDBitSets[i];
			flag |= cd != null;

			Collection cdp = new ArrayList();
			BasicBlock bb = (BasicBlock) nodes.get(i);

			for (Iterator j = bb.getStmtsOf().iterator(); j.hasNext();) {
				mDependee.set(sl.indexOf(j.next()), cdp);
			}

			for (int j = cd.nextSetBit(0); j != -1; j = cd.nextSetBit(j + 1)) {
				BasicBlock cdbb = (BasicBlock) nodes.get(j);
				Object cdStmt;

				if (forward) {
					cdStmt = cdbb.getLeaderStmt();
				} else {
					cdStmt = cdbb.getTrailerStmt();
				}

				cdp.add(cdStmt);

				int deIndex = sl.indexOf(cdStmt);
				Collection dees = (Collection) mDependent.get(deIndex);

				if (dees == null) {
					dees = new ArrayList();
					mDependent.set(deIndex, dees);
				}
				dees.add(bb.getStmtsOf());
			}
		}

		if (flag) {
			dependentMap.put(method, new ArrayList(mDependent));
			dependeeMap.put(method, new ArrayList(mDependee));
		} else {
			dependentMap.put(method, null);
			dependeeMap.put(method, null);
		}
	}
}

/*
   ChangeLog:
   $Log$
   Revision 1.23  2003/11/06 05:15:07  venku
   - Refactoring, Refactoring, Refactoring.
   - Generalized the processing controller to be available
     in Indus as it may be useful outside static anlaysis. This
     meant moving IProcessor, Context, and ProcessingController.
   - ripple effect of the above changes was large.
   Revision 1.22  2003/11/05 09:29:51  venku
   - ripple effect of splitting IWorkBag.
   Revision 1.21  2003/11/05 04:25:34  venku
   - return value of getDependees() was type incorrect.  FIXED.
   Revision 1.20  2003/11/05 04:20:05  venku
   - formatting.
   Revision 1.19  2003/11/05 04:17:28  venku
   - subtle bug caused when enabled bi-directional support. FIXED.
   Revision 1.18  2003/11/05 00:44:51  venku
   - added logging statements to track the execution.
   Revision 1.17  2003/11/03 07:56:04  venku
   - added logging.
   Revision 1.16  2003/10/31 01:00:58  venku
   - added support to switch direction.  However, forward
     slicing can be viewed in two interesting ways and
     our implementation handles the most interesting
     direction.
   Revision 1.15  2003/09/28 12:27:31  venku
   -  The control dep was buggy. FIXED.
   Revision 1.14  2003/09/28 06:20:38  venku
   - made the core independent of hard code used to create unit graphs.
     The core depends on the environment to provide a factory that creates
     these unit graphs.
   Revision 1.13  2003/09/28 03:16:48  venku
   - I don't know.  cvs indicates that there are no differences,
     but yet says it is out of sync.
   Revision 1.12  2003/09/16 08:27:35  venku
   - Well, we calculated doms, not idoms.  FIXED.
   Revision 1.11  2003/09/16 05:54:56  venku
   - changed access specifiers of methods from protected to private
     as they were not being called in the package or subclasses.
   Revision 1.10  2003/09/15 01:22:06  venku
   - fixupMaps() was screwed. FIXED.
   Revision 1.9  2003/09/15 00:58:25  venku
   - well, things were fine I guess. Nevertheless, they are more
     streamlined now.
   Revision 1.8  2003/09/14 23:24:26  venku
   - alas a working control DA. However, I have not been able
     to compile a program such that the basic block has two CD points.
     This is possible when the else branch of the enclosed and enclosing
     if's are identical.
   Revision 1.7  2003/09/13 05:56:34  venku
   - an early commit to a (hopefully) working solution.
   - need to document it still.
   Revision 1.6  2003/09/12 23:49:46  venku
   - another one of those unsuccessful solutions.  Checking in to start over.
   Revision 1.5  2003/08/11 06:34:52  venku
   Changed format of change log accumulation at the end of the file
   Revision 1.4  2003/08/11 06:31:55  venku
   Changed format of change log accumulation at the end of the file
   Revision 1.3  2003/08/11 04:20:19  venku
   - Pair and Triple were changed to work in optimized and unoptimized mode.
   - Ripple effect of the previous change.
   - Documentation and specification of other classes.
   Revision 1.2  2003/08/09 23:29:52  venku
   Ripple Effect of renaming Inter/Intra procedural data DAs to Aliased/NonAliased data DA.
   Revision 1.1  2003/08/07 06:38:05  venku
   Major:
    - Moved the packages under indus umbrella.
    - Renamed MethodLocalDataDA to NonAliasedDataDA.
    - Added class for AliasedDataDA.
    - Documented and specified the classes.
 */
