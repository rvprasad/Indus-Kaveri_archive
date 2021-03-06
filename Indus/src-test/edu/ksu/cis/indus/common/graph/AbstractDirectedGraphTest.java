
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

package edu.ksu.cis.indus.common.graph;

import edu.ksu.cis.indus.IndusTestCase;
import edu.ksu.cis.indus.common.collections.ITransformer;
import edu.ksu.cis.indus.common.collections.SetUtils;
import edu.ksu.cis.indus.common.datastructures.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * This class tests <code>DirectedGraph</code> class that exists in the same package. Methods which conduct tests specific to
 * the setup data are named as <code>localtestXXX()</code> and these will be called from <code>testXXX()</code> methods. So,
 * subclasses which setup diffferent graph should override this method suitably.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 *
 * @see edu.ksu.cis.indus.common.graph.IDirectedGraph
 */
public abstract class AbstractDirectedGraphTest
  extends IndusTestCase {
	/** 
	 * This is the graph that will be tested.
	 */
	protected IDirectedGraph dg;

	/**
	 * Tests <code>addEdgeFromTo</code> method.
	 */
	public final void testAddEdgeFromTo() {
		localtestAddEdgeFromTo();
	}

	/**
	 * Tests <code>getBackEdges()</code> method.
	 */
	public final void testGetBackEdges() {
		final Collection _cycles = dg.getCycles();
		final Collection _backedges = dg.getBackEdges();
		final Collection _nodes = new HashSet();

		assertTrue(_cycles.isEmpty() == _backedges.isEmpty());

		// ensure that all nodes in the edge do belong to the graph.
		for (final Iterator _i = _backedges.iterator(); _i.hasNext();) {
			final Pair _edge = (Pair) _i.next();
			_nodes.add(_edge.getFirst());
			_nodes.add(_edge.getSecond());
		}
		assertTrue(dg.getNodes().containsAll(_nodes));

		for (final Iterator _i = _backedges.iterator(); _i.hasNext();) {
			final Pair _edge = (Pair) _i.next();
			final INode _src = (INode) _edge.getFirst();
			final INode _dest = (INode) _edge.getSecond();
			assertTrue(_src.getSuccsOf().contains(_dest));
			assertTrue(dg.isReachable(_dest, _src, true));

			boolean _flag = false;

			// ensure that the edge belongs to atleast one cycle.
			for (final Iterator _j = _cycles.iterator(); _j.hasNext();) {
				final Collection _cycle = (Collection) _j.next();
				_flag |= _cycle.contains(_src);
				_flag |= _cycle.contains(_dest);
			}
			assertTrue(_flag);
		}
	}

	/**
	 * Tests <code>getCommonReachablesFrom</code> method.
	 */
	public final void testGetCommonsReachablesFrom() {
		final boolean[] _flags = { false, true };

		for (final Iterator _i = dg.getNodes().iterator(); _i.hasNext();) {
			final INode _n1 = (INode) _i.next();

			for (final Iterator _j = dg.getNodes().iterator(); _j.hasNext();) {
				final INode _n2 = (INode) _j.next();

				for (int _k = 0; _k <= 1; _k++) {
					final Collection _c1 = dg.getReachablesFrom(_n1, _flags[_k]);

					for (int _l = 0; _l <= 1; _l++) {
						final Collection _c2 = dg.getReachablesFrom(_n2, _flags[_l]);
						final Collection _c3 = dg.getCommonReachablesFrom(_n1, _flags[_k], _n2, _flags[_l]);
						assertTrue(_c3.containsAll(SetUtils.intersection(_c2, _c1)));
						assertTrue(SetUtils.intersection(_c2, _c1).containsAll(_c3));
					}
				}
			}
		}
	}

	/**
	 * Tests <code>getCycles()</code> method.
	 */
	public final void testGetCycles() {
		final Collection _cycles = dg.getCycles();

		for (final Iterator _i = _cycles.iterator(); _i.hasNext();) {
			final Collection _cycle = (Collection) _i.next();
			final INode _node = (INode) _cycle.iterator().next();
			assertTrue(dg.isReachable(_node, _node, true));
		}
		localtestGetCycles();
	}

	/**
	 * Tests <code>getDAG()</code> method.
	 */
	public final void testGetDAG() {
		final IDirectedGraph _dag = dg.getDAG();
		final List _sccTD = _dag.getSCCs(true);
		final Iterator _i = _sccTD.iterator();
		final int _iEnd = _sccTD.size();

		for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
			final Collection _scc = (Collection) _i.next();
			assertTrue(_scc.size() == 1);
		}
		assertTrue(_dag.getCycles() + " ", _dag.getCycles().isEmpty());
	}

	/**
	 * Tests <code>getSCCs()</code> method.
	 */
	public final void testGetSCCs() {
		final Collection _sccsTrue = dg.getSCCs(true);
		checkSCCReachability(_sccsTrue);

		final Collection _sccsFalse = dg.getSCCs(false);
		checkSCCReachability(_sccsFalse);
		assertTrue(_sccsTrue.containsAll(_sccsFalse));
		assertTrue(_sccsFalse.containsAll(_sccsTrue));
		localtestGetSCCs();
	}

	/**
	 * Tests <code>getSinks()</code> method.
	 */
	public final void testGetSinks() {
		final Collection _sinks = dg.getSinks();

		// ensure none of the tails have a successor
		for (final Iterator _i = _sinks.iterator(); _i.hasNext();) {
			final INode _node = (INode) _i.next();
			assertTrue(_node.getSuccsOf().isEmpty());
		}

		// ensure none of the nodes have a tail node as a predecessor
		for (final Iterator _i = dg.getNodes().iterator(); _i.hasNext();) {
			final INode _node = (INode) _i.next();
			assertTrue(SetUtils.intersection(_node.getPredsOf(), _sinks).isEmpty());
		}
	}

	/**
	 * Tests <code>getSources()</code> method.
	 */
	public final void testGetSources() {
		final Collection _sources = dg.getSources();

		// ensure there are no predecessors for the head nodes
		for (final Iterator _i = _sources.iterator(); _i.hasNext();) {
			final INode _node = (INode) _i.next();
			assertTrue(_node.getPredsOf().isEmpty());
		}

		// ensure none of the nodes have a head node as their successor
		for (final Iterator _i = dg.getNodes().iterator(); _i.hasNext();) {
			final INode _node = (INode) _i.next();
			assertTrue(SetUtils.intersection(_node.getSuccsOf(), _sources).isEmpty());
		}
	}

	/**
	 * Tests <code>getSpanningSuccs()</code> method.
	 */
	public final void testGetSpanningSuccs() {
		final IObjectDirectedGraph _forest = dg.getSpanningForest();
		final Collection<INode> _nodes = _forest.getNodes();
		
		// ensure all tree nodes are among the nodes of this graph.
		final Collection _dgNodes = new HashSet();
		final ITransformer _trans = _forest.getObjectExtractor();
		for(Object _o : _nodes) {
			_dgNodes.add(_trans.transform(_o));
		}
		assertTrue(dg.getNodes().containsAll(_dgNodes));

		// ensure no node appears as child of two different parents in the spanning tree.
		for (final INode _n1 : _nodes) {
			assertTrue(_n1.getPredsOf().size() <= 1);
			for (final INode _n2 : _nodes) {
				if (!_n1.equals(_n2)) {
					assertTrue(SetUtils.intersection(_n1.getSuccsOf(), _n2.getSuccsOf()).isEmpty());
				}
			}
		}

		// ensure that all nodes occur in the spanning tree
		assertTrue(_dgNodes.containsAll(dg.getNodes()));
	}

	/**
	 * Tests <code>isAncestorOf()</code> method.
	 */
	public final void testIsAncestorOf() {
		localtestIsAncestorOf();
	}

	/**
	 * Tests <code>isReachable()</code> method.
	 */
	public final void testIsReachable() {
		localtestIsReachable();
	}

	/**
	 * Tests <code>performTopologicalSort()</code> method.
	 */
	public final void testPerformTopologicalSort() {
		final List _sorted = dg.performTopologicalSort(true);
		final List _nodes = dg.getNodes();
		assertTrue(_nodes.containsAll(_sorted) && _sorted.containsAll(_nodes));

		if (dg.getCycles().isEmpty()) {
			for (final Iterator _i = _sorted.iterator(); _i.hasNext();) {
				final INode _src = (INode) _i.next();
				final Collection _t = _sorted.subList(_sorted.indexOf(_src), _sorted.size() - 1);

				for (final Iterator _j = _t.iterator(); _j.hasNext();) {
					final INode _dest = (INode) _j.next();
					assertFalse(dg.isReachable(_dest, _src, true));
				}
			}
		}
	}

	/**
	 * Tests <code>getReachablesFrom()</code> method.
	 */
	public final void testgetReachablesFrom() {
		final Collection _nodes = dg.getNodes();

		for (final Iterator _i = _nodes.iterator(); _i.hasNext();) {
			final INode _src = (INode) _i.next();
			final Collection _fReachables = dg.getReachablesFrom(_src, true);
			final Collection _rReachables = dg.getReachablesFrom(_src, false);

			for (final Iterator _j = _nodes.iterator(); _j.hasNext();) {
				final INode _dest = (INode) _j.next();
				assertTrue(_fReachables.contains(_dest) == dg.isReachable(_src, _dest, true));
				assertTrue(_rReachables.contains(_dest) == dg.isReachable(_src, _dest, false));
			}
		}
	}

	/**
	 * @see edu.ksu.cis.indus.common.graph.AbstractDirectedGraphTest#testGetTails()
	 */
	public void testGetTails() {
		assertTrue(dg.getTails().containsAll(dg.getSinks()));
	}

	/**
	 * Extracts the predecessors and successors of the given graph into the given maps.
	 *
	 * @param graph from which to extract information.
	 * @param preds will contain a node to predecessor mapping on return.
	 * @param succs will contain a node to successor mapping on return.
	 *
	 * @pre graph != null and preds != null and succs != null
	 * @post preds.oclIsKindOf(INode, Collection(INode)) and succs.oclIsKindOf(INode, Collection(INode))
	 */
	protected final void extractPredSuccCopy(final IDirectedGraph graph, final Map preds, final Map succs) {
		for (final Iterator _i = graph.getNodes().iterator(); _i.hasNext();) {
			final INode _node = (INode) _i.next();
			preds.put(_node, new ArrayList(_node.getPredsOf()));
			succs.put(_node, new ArrayList(_node.getSuccsOf()));
		}
	}

	///CLOVER:OFF

	/**
	 * Tests <code>addEdgeFromTo</code> method on test local graph instance.
	 */
	protected void localtestAddEdgeFromTo() {
	}

	/**
	 * Checks <code>getCycles()</code> on the local graph instance.
	 */
	protected void localtestGetCycles() {
	}

	/**
	 * Checks <code>getSCCs()</code> on the local graph instance.
	 */
	protected void localtestGetSCCs() {
	}

	/**
	 * Tests <code>isAncestorOf()</code> method.
	 */
	protected void localtestIsAncestorOf() {
	}

	/**
	 * Checks <code>isReachable()</code> on the local graph instance.
	 */
	protected void localtestIsReachable() {
	}

	///CLOVER:ON

	/**
	 * Checks for the reachability of nodes in the SCCs.
	 *
	 * @param sccs in which reachability should be checked.
	 *
	 * @pre sccs != null
	 */
	private void checkSCCReachability(final Collection sccs) {
		final Collection _nodes = new HashSet();
		final Collection _rest = new HashSet();

		for (final Iterator _i = sccs.iterator(); _i.hasNext();) {
			final Collection _scc = (Collection) _i.next();
			_nodes.addAll(_scc);

			if (_scc.size() > 1) {
				for (final Iterator _j = _scc.iterator(); _j.hasNext();) {
					final INode _srcNode = (INode) _j.next();
					_rest.clear();
					_rest.addAll(_scc);
					_rest.remove(_srcNode);

					for (final Iterator _k = _rest.iterator(); _k.hasNext();) {
						final INode _destNode = (INode) _k.next();
						assertTrue(dg.isReachable(_srcNode, _destNode, true) && dg.isReachable(_srcNode, _destNode, false));
					}
				}
			}
		}
		assertTrue(dg.getNodes().containsAll(_nodes));
	}
}

// End of File
