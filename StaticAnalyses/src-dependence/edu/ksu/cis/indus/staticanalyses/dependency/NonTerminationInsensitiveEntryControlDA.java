
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

package edu.ksu.cis.indus.staticanalyses.dependency;

import edu.ksu.cis.indus.common.collections.CollectionsUtilities;
import edu.ksu.cis.indus.common.datastructures.HistoryAwareFIFOWorkBag;
import edu.ksu.cis.indus.common.datastructures.IWorkBag;
import edu.ksu.cis.indus.common.graph.IDirectedGraph;
import edu.ksu.cis.indus.common.graph.INode;
import edu.ksu.cis.indus.common.soot.BasicBlockGraph;
import edu.ksu.cis.indus.common.soot.BasicBlockGraph.BasicBlock;
import edu.ksu.cis.indus.common.soot.BasicBlockGraphMgr;

import edu.ksu.cis.indus.interfaces.ICallGraphInfo;

import edu.ksu.cis.indus.staticanalyses.InitializationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import soot.SootMethod;

import soot.jimple.Stmt;


/**
 * This class provides intraprocedural non-termination sensitive backward control dependence information based on the
 * indirect version of non-termination sensitive backward control dependence.  For more information about the dependence
 * calculated in  this implementation, please refer to  <a
 * href="http://projects.cis.ksu.edu/docman/view.php/12/95/santos-tr2004-8.pdf">Santos-TR2004-8</a>.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public final class NonTerminationInsensitiveEntryControlDA
  extends AbstractControlDA {
	/** 
	 * The logger used by instances of this class to log messages.
	 */
	private static final Log LOGGER = LogFactory.getLog(NonTerminationInsensitiveEntryControlDA.class);

	/** 
	 * The instance of analysis that provides backward control dependence information.
	 */
	private final NonTerminationSensitiveEntryControlDA entryControlDA = new NonTerminationSensitiveEntryControlDA();

	/** 
	 * This indicates which version, direct or indirect, of non-termination sensitive backward dependence should be used as
	 * the basis of this analysis.
	 */
	private final boolean useIndirectBackwardDependence;

	/**
	 * Creates a new NonTerminationInsensitiveEntryControlDA object in which the indirect version of non-termination
	 * sensitive backward dependence will be used.
	 */
	public NonTerminationInsensitiveEntryControlDA() {
		useIndirectBackwardDependence = true;
	}

	/**
	 * Creates an instance of this class.
	 *
	 * @param indirect <code>true</code> indicates that indirect version of non-termination sensitive  backward dependence
	 * 		  should be used as the basis of this analysis; <code>false</code> indicates the direct version of
	 * 		  non-termination sensitive  backward dependence should be used as the basis of this analysis.  <i>Please note
	 * 		  that this  constructor is  provided for experimentation only.  As discussed in <a
	 * 		  href="http://projects.cis.ksu.edu/docman/view.php/12/95/santos-tr2004-8.pdf">Santos-TR2004-8</a>, only the
	 * 		  results based on the indirect non-termination sensitive backward dependence will be  complete.</i>
	 */
	public NonTerminationInsensitiveEntryControlDA(final boolean indirect) {
		super();
		this.useIndirectBackwardDependence = indirect;
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.AbstractAnalysis#setBasicBlockGraphManager(BasicBlockGraphMgr)
	 */
	public void setBasicBlockGraphManager(final BasicBlockGraphMgr bbm) {
		super.setBasicBlockGraphManager(bbm);
		entryControlDA.setBasicBlockGraphManager(bbm);
	}

	/**
	 * {@inheritDoc} This implementation will return <code>BACKWARD_DIRECTION</code>.
	 */
	public Object getDirection() {
		return BACKWARD_DIRECTION;
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.IDependencyAnalysis#getIndirectVersionOfDependence()
	 */
	public IDependencyAnalysis getIndirectVersionOfDependence() {
		return new IndirectDependenceAnalysis(this, IDependenceRetriever.STMT_DEP_RETRIEVER);
	}

	/**
	 * Calculates the control dependency information for the methods provided during initialization.
	 *
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.AbstractDependencyAnalysis#analyze()
	 */
	public void analyze() {
		analyze(callgraph.getReachableMethods());
	}

	/**
	 * Calculates the control dependency information for the provided methods.  The use of this method does not require a
	 * prior call to <code>setup</code>.
	 *
	 * @param methods to be analyzed.
	 *
	 * @pre methods != null and methods.oclIsKindOf(Collection(SootMethod)) and not method->includes(null)
	 */
	public void analyze(final Collection methods) {
		unstable();

		entryControlDA.analyze(methods);

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("BEGIN: Entry Control Dependence processing");
		}

		final IDependencyAnalysis _nda;

		if (useIndirectBackwardDependence) {
			_nda = entryControlDA.getIndirectVersionOfDependence();
		} else {
			_nda = entryControlDA;
		}

		for (final Iterator _i = methods.iterator(); _i.hasNext();) {
			final SootMethod _method = (SootMethod) _i.next();
			copyIndirectDAInfo(_method, _nda);
			processMethod(_method);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(toString());
		}

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("END: Entry Control Dependence processing");
		}

		stable();
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

		super.setup();
		callgraph = (ICallGraphInfo) info.get(ICallGraphInfo.ID);

		if (callgraph == null) {
			throw new InitializationException(ICallGraphInfo.ID + " was not provided.");
		}

		entryControlDA.initialize(info);
	}

	/**
	 * Retrieves the control sinks in the given graph.
	 *
	 * @param graph of interest.
	 *
	 * @return a collection of control sinks.
	 *
	 * @pre graph != null
	 * @post result != null and result.oclIsKindOf(Collection(Collection(INode)))
	 * @post result->forall(o | o->forall(p | graph.getNodes().contains(p)))
	 */
	private Collection getControlSinksOf(final IDirectedGraph graph) {
		final Collection _result = new ArrayList();
		final Collection _temp = new ArrayList();
		final Collection _sccs = graph.getSCCs(true);
		final Iterator _i = _sccs.iterator();
		final int _iEnd = _sccs.size();

		for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
			final Collection _scc = (Collection) _i.next();
			_temp.clear();

			final Iterator _j = _scc.iterator();
			final int _sccSize = _scc.size();

			for (int _jIndex = 0; _jIndex < _sccSize; _jIndex++) {
				final INode _node = (INode) _j.next();
				_temp.addAll(_node.getSuccsOf());
			}

			if (_temp.equals(_scc) || (_sccSize == 1 && _temp.isEmpty())) {
				_result.add(_scc);
			}
		}
		return _result;
	}

	/**
	 * Retrieves the nodes belonging to the control sinks that do not contain the given node.
	 *
	 * @param node of interest.
	 * @param sinks a collection of control sinks.
	 *
	 * @return a collection of nodes.
	 *
	 * @pre node != null and sinks != null
	 * @pre sinks.oclIsKindOf(Collection(Collection(INode)))
	 * @post result != null and result.oclIsKindOf(Collection(INode))
	 * @post result->foreach(o | sinks->exists(p | p.contains(o) and not p.contains(node)))
	 */
	private Collection getNodesOfSinksNotContainingNode(final INode node, final Collection sinks) {
		final Collection _result = new ArrayList();
		final Iterator _i = sinks.iterator();
		final int _iEnd = sinks.size();

		for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
			final Collection _sinkNodes = (Collection) _i.next();

			if (!_sinkNodes.contains(node)) {
				_result.addAll(_sinkNodes);
			}
		}
		return _result;
	}

	/**
	 * Copies the dependency information for the given method from da into the data structures of this instance.
	 *
	 * @param method of interest.
	 * @param da from which to copy information.
	 *
	 * @pre method != null and da != null
	 */
	private void copyIndirectDAInfo(final SootMethod method, final IDependencyAnalysis da) {
		final List _dee2Dent = CollectionsUtilities.getListFromMap(dependee2dependent, method);
		final List _dent2Dee = CollectionsUtilities.getListFromMap(dependent2dependee, method);
		final List _stmtList = getStmtList(method);
		final int _noOfStmtsInMethod = _stmtList.size();
		CollectionsUtilities.ensureSize(_dee2Dent, _noOfStmtsInMethod, null);
		CollectionsUtilities.ensureSize(_dent2Dee, _noOfStmtsInMethod, null);

		final BasicBlockGraph _bbg = getBasicBlockGraph(method);
		final List _nodes = _bbg.getNodes();
		final Iterator _i = _nodes.iterator();
		final int _iEnd = _nodes.size();

		for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
			final BasicBlock _bb = (BasicBlock) _i.next();
			final Stmt _dependee = _bb.getTrailerStmt();
			final Collection _dependents = da.getDependents(_dependee, method);
			final Collection _stmtLevelDependentSet =
				(Collection) CollectionsUtilities.getSetAtIndexFromList(_dee2Dent, _stmtList.indexOf(_dependee));
			_stmtLevelDependentSet.addAll(_dependents);

			final Iterator _k = _dependents.iterator();
			final int _kEnd = _dependents.size();

			for (int _kIndex = 0; _kIndex < _kEnd; _kIndex++) {
				final Stmt _dent = (Stmt) _k.next();
				final Collection _stmtLevelDependeeSet =
					(Collection) CollectionsUtilities.getSetAtIndexFromList(_dent2Dee, _stmtList.indexOf(_dent));
				_stmtLevelDependeeSet.add(_dependee);
			}
		}
	}

	/**
	 * Analyzes the given method.
	 *
	 * @param method to be analyzed.
	 *
	 * @pre method != null
	 */
	private void processMethod(final SootMethod method) {
		final List _methodLocalDee2Dent = CollectionsUtilities.getListFromMap(dependee2dependent, method);
		final List _methodLocalDent2Dee = CollectionsUtilities.getListFromMap(dependent2dependee, method);
		final List _stmtList = getStmtList(method);
		final BasicBlockGraph _bbg = getBasicBlockGraph(method);
		final Collection _sinks = getControlSinksOf(_bbg);
		final Collection _dependees = new ArrayList();
		final List _nodes = _bbg.getNodes();
		final Iterator _i = _nodes.iterator();
		final int _iEnd = _nodes.size();

		for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
			final BasicBlock _dependentBB = (BasicBlock) _i.next();
			final Collection _sinkNodes = getNodesOfSinksNotContainingNode(_dependentBB, _sinks);

			// we use a temporary copy of dependees as we will be updating the orginal collection.
			_dependees.clear();
			_dependees.addAll(getDependees(_dependentBB.getLeaderStmt(), method));

			final Iterator _j = _dependees.iterator();
			final int _jEnd = _dependees.size();

			for (int _jIndex = 0; _jIndex < _jEnd; _jIndex++) {
				final Stmt _dependeeStmt = (Stmt) _j.next();
				final BasicBlock _dependeeBB = _bbg.getEnclosingBlock(_dependeeStmt);

				if (shouldRemoveDependenceBetween(_dependeeBB, _dependentBB, _sinkNodes)) {
					removeDependenceOfOn(_dependentBB, _dependeeBB, _methodLocalDee2Dent, _methodLocalDent2Dee, _stmtList);
				}
			}
		}
	}

	/**
	 * Removes the dependence of statements in <code>dependentBB</code> on statemetns in <code>dependeeBB</code>. It removes
	 * the dependence information in both directions.
	 *
	 * @param dependentBB is the basic block containing the dependent statements.
	 * @param dependeeBB is the basic block containing the dependee statements.
	 * @param methodLocalDee2Dent is the map for dependee to dependent that is to be updated.
	 * @param methodLocalDent2Dee is the map for dependent to dependee that is to be updated.
	 * @param stmtList is the list of statements.
	 *
	 * @pre dependentBB != null and dependeeBB != null and methodLocalDee2Dent != null and methodLocalDent2Dee != null and
	 * 		stmtList != null
	 */
	private void removeDependenceOfOn(final BasicBlock dependentBB, final BasicBlock dependeeBB,
		final List methodLocalDee2Dent, final List methodLocalDent2Dee, final List stmtList) {
		final Stmt _deeStmt = dependeeBB.getTrailerStmt();
		final Collection _stmtLevelDependentSet =
			(Collection) CollectionsUtilities.getSetAtIndexFromList(methodLocalDee2Dent, stmtList.indexOf(_deeStmt));
		final List _stmtsOf = dependentBB.getStmtsOf();
		_stmtLevelDependentSet.removeAll(_stmtsOf);

		final Iterator _i = _stmtsOf.iterator();
		final int _iEnd = _stmtsOf.size();

		for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
			final Stmt _dentStmt = (Stmt) _i.next();
			final Collection _stmtLevelDependeeSet =
				(Collection) CollectionsUtilities.getSetAtIndexFromList(methodLocalDent2Dee, stmtList.indexOf(_dentStmt));
			_stmtLevelDependeeSet.remove(_deeStmt);
		}
	}

	/**
	 * Checks if the dependence relation should be removed between statements in the given basic blocks.
	 *
	 * @param dependeeBB contains the dependee statement.
	 * @param dependentBB contains the dependent statements.
	 * @param sinkNodes is the collection of nodes that belong to control sinks of the basic block graph.
	 *
	 * @return <code>true</code> if the dependence relation should be removed; <code>false</code>, otherwise.
	 *
	 * @pre dependeeBB != null and dependentBB != null and sinkNodes != null
	 * @pre sinkNodes.oclIsKindOf(Collection(BasicBlock))
	 */
	private boolean shouldRemoveDependenceBetween(final BasicBlock dependeeBB, final BasicBlock dependentBB,
		final Collection sinkNodes) {
		final Collection _visited = new HashSet();
		final IWorkBag _wb = new HistoryAwareFIFOWorkBag(_visited);
		boolean _notcd = true;
		_visited.add(dependentBB);
		_wb.addWork(dependeeBB);

		while (_wb.hasWork() && _notcd) {
			final BasicBlock _bb = (BasicBlock) _wb.getWork();

			if (sinkNodes.contains(_bb)) {
				_notcd = false;
			} else {
				_wb.addAllWorkNoDuplicates(_bb.getSuccsOf());
			}
		}
		return _notcd;
	}
}

// End of File