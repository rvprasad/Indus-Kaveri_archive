
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

package edu.ksu.cis.indus.common.graph;

import edu.ksu.cis.indus.common.graph.SimpleNodeGraph.SimpleNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Tests exception cases in <code>SimpleNodeGraph</code>.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class SimpleNodeGraphTest
  extends AbstractDirectedGraphTest {
	/** 
	 * This maps node names to nodes.
	 *
	 * @invariant name2node.oclIsKindOf(Map(String, INode))
	 */
	protected final Map name2node = new HashMap();

	/** 
	 * The reference to a simple node graph used for testing.  This should be the same as dg, so set this via
	 * <code>setSNG</code>.
	 *
	 * @invariant dg = sng
	 */
	protected SimpleNodeGraph sng;

	/**
	 * Tests <code>getNode</code> method.
	 */
	public final void testGetNode() {
		for (final Iterator _i = name2node.entrySet().iterator(); _i.hasNext();) {
			final Map.Entry _entry = (Map.Entry) _i.next();
			final INode _node = sng.getNode(_entry.getKey());
			assertTrue(_node.equals(_entry.getValue()));
			assertTrue(((SimpleNode) _node).getObject().equals(_entry.getKey()));
		}

		try {
			sng.getNode(null);
			fail("Should have raised an exception.");
		} catch (NullPointerException _e) {
			;
		}
	}

	/**
	 * Tests <code>getNodes</code> method.
	 */
	public void testGetNodes() {
		final List _nodes1 = dg.getNodes();

		// check for bidirectional containment to establish equality.
		assertTrue(name2node.values().containsAll(dg.getNodes()));
		assertTrue(dg.getNodes().containsAll(name2node.values()));

		final List _nodes2 = dg.getNodes();

		// check ordering across calls is same..
		assertTrue(_nodes1.equals(_nodes2));
	}

	/**
	 * Tests <code>getNodesInPathBetween()</code>.
	 */
	public void testGetNodesInPathBetween() {
		final Collection _t = new HashSet();
		_t.add(sng.getNode("b"));
		_t.add(sng.getNode("e"));

		final Collection _nodes = dg.getNodesOnPathBetween(_t);
		_t.add(sng.getNode("a"));
		assertTrue(_nodes + " " + _t, _nodes.containsAll(_t) && _t.containsAll(_nodes));
	}

	/**
	 * Set the graph to be tested.
	 *
	 * @param graph is the graph to be tested.
	 *
	 * @pre graph != null
	 * @post sng = graph and dg = graph
	 */
	protected void setSNG(final SimpleNodeGraph graph) {
		sng = graph;
		dg = sng;
	}

	/**
	 * We construct the graph given in the book "Introduction to Algorithms" on page 553.
	 *
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp()
	  throws Exception {
		final SimpleNodeGraph _sng = new SimpleNodeGraph();

		name2node.put("a", _sng.getNode("a"));
		name2node.put("b", _sng.getNode("b"));
		name2node.put("c", _sng.getNode("c"));
		name2node.put("d", _sng.getNode("d"));
		name2node.put("e", _sng.getNode("e"));
		name2node.put("f", _sng.getNode("f"));
		name2node.put("g", _sng.getNode("g"));
		name2node.put("h", _sng.getNode("h"));
		// connect them now
		_sng.addEdgeFromTo((SimpleNode) name2node.get("a"), (SimpleNode) name2node.get("b"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("b"), (SimpleNode) name2node.get("c"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("b"), (SimpleNode) name2node.get("f"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("b"), (SimpleNode) name2node.get("e"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("c"), (SimpleNode) name2node.get("d"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("c"), (SimpleNode) name2node.get("g"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("d"), (SimpleNode) name2node.get("c"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("d"), (SimpleNode) name2node.get("h"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("e"), (SimpleNode) name2node.get("f"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("e"), (SimpleNode) name2node.get("a"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("f"), (SimpleNode) name2node.get("g"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("g"), (SimpleNode) name2node.get("h"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("g"), (SimpleNode) name2node.get("f"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("h"), (SimpleNode) name2node.get("h"));
		setSNG(_sng);
	}

	/**
	 * @see AbstractDirectedGraphTest#localtestAddEdgeFromTo
	 */
	protected void localtestAddEdgeFromTo() {
		final Map _preds1 = new HashMap();
		final Map _succs1 = new HashMap();
		extractPredSuccCopy(dg, _preds1, _succs1);

		// Add edge from c to h
		sng.addEdgeFromTo((SimpleNode) name2node.get("c"), (SimpleNode) name2node.get("h"));

		assertTrue(((INode) name2node.get("c")).getSuccsOf().contains(name2node.get("h")));
		assertTrue(((INode) name2node.get("h")).getPredsOf().contains(name2node.get("c")));

		final Map _preds2 = new HashMap();
		final Map _succs2 = new HashMap();
		extractPredSuccCopy(dg, _preds2, _succs2);

		for (final Iterator _i = dg.getNodes().iterator(); _i.hasNext();) {
			final INode _node = (INode) _i.next();

			if (name2node.get("h") != _node) {
				assertTrue(_preds1.get(_node).equals(_preds2.get(_node)));
			}

			if (name2node.get("c") != _node) {
				assertTrue(_succs1.get(_node).equals(_succs2.get(_node)));
			}
		}

		// Add edge from a to f
		sng.addEdgeFromTo((SimpleNode) name2node.get("a"), (SimpleNode) name2node.get("f"));

		_preds1.clear();
		_succs1.clear();
		extractPredSuccCopy(dg, _preds1, _succs1);

		assertTrue(((INode) name2node.get("a")).getSuccsOf().contains(name2node.get("f")));
		assertTrue(((INode) name2node.get("f")).getPredsOf().contains(name2node.get("a")));

		for (final Iterator _i = dg.getNodes().iterator(); _i.hasNext();) {
			final INode _node = (INode) _i.next();

			if (name2node.get("f") != _node) {
				assertTrue(_preds1.get(_node).equals(_preds2.get(_node)));
			}

			if (name2node.get("a") != _node) {
				assertTrue(_succs1.get(_node).equals(_succs2.get(_node)));
			}
		}

		// Adding edges between non-existent nodes
		try {
			assertFalse(sng.addEdgeFromTo(sng.new SimpleNode("t1"), sng.new SimpleNode("t2")));
			fail("This should not happen!");
		} catch (final IllegalArgumentException _e) {
			;
		}

		// Create Spanning tree, add a new node, and check if the a new spanning tree will be built.
		final Map _temp = new HashMap(dg.getSpanningSuccs());
		assertTrue(sng.addEdgeFromTo(sng.getNode("i"), sng.getNode("b")));
		assertFalse(_temp.equals(dg.getSpanningSuccs()));
	}

	/**
	 * @see edu.ksu.cis.indus.common.graph.AbstractDirectedGraphTest#localtestGetCycles()
	 */
	protected void localtestGetCycles() {
		final Collection _cycles = dg.getCycles();
        assertFalse(_cycles.isEmpty());
		final List _cycle = new ArrayList();
		_cycle.add(this.sng.getNode("a"));
		_cycle.add(this.sng.getNode("b"));
		_cycle.add(this.sng.getNode("e"));
		assertTrue(_cycles.contains(_cycle));
	}

	/**
	 * @see AbstractDirectedGraphTest#localtestGetHeads
	 */
	protected void localtestGetHeads() {
		assertTrue(dg.getHeads().isEmpty());
	}

	/**
	 * @see edu.ksu.cis.indus.common.graph.AbstractDirectedGraphTest#localtestGetPseudoTails()
	 */
	protected void localtestGetPseudoTails() {
		final Collection _dtails = dg.getPseudoTails();
		assertTrue(_dtails.size() == 1);
		assertTrue(_dtails.contains(name2node.get("h")));
	}

	/**
	 * @see edu.ksu.cis.indus.common.graph.AbstractDirectedGraphTest#localtestGetSCCs()
	 */
	protected void localtestGetSCCs() {
		final Collection _sccsTrue = dg.getSCCs(true);
		final Collection _sccsFalse = dg.getSCCs(false);
		assertFalse(_sccsTrue.isEmpty());
		assertFalse(_sccsFalse.isEmpty());
	}

	/**
	 * @see AbstractDirectedGraphTest#localtestGraphGetTails
	 */
	protected void localtestGraphGetTails() {
		assertTrue(dg.getTails().isEmpty());
	}

	/**
	 * @see AbstractDirectedGraphTest#localtestIsAncestorOf
	 */
	protected void localtestIsAncestorOf() {
		assertTrue(dg.isAncestorOf((INode) name2node.get("a"), (INode) name2node.get("a")));
		assertTrue(dg.isAncestorOf((INode) name2node.get("h"), (INode) name2node.get("h")));
	}

	/**
	 * @see AbstractDirectedGraphTest#localtestIsReachable
	 */
	protected void localtestIsReachable() {
		assertTrue(dg.isReachable((INode) name2node.get("a"), (INode) name2node.get("e"), true));
		assertFalse(dg.isReachable((INode) name2node.get("f"), (INode) name2node.get("c"), true));
		assertTrue(dg.isReachable((INode) name2node.get("h"), (INode) name2node.get("a"), false));
		assertFalse(dg.isReachable((INode) name2node.get("c"), (INode) name2node.get("h"), false));
		assertTrue(dg.isReachable((INode) name2node.get("h"), (INode) name2node.get("h"), false));
		assertTrue(dg.isReachable((INode) name2node.get("h"), (INode) name2node.get("h"), true));
	}

	/**
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown()
	  throws Exception {
		dg = null;
		sng = null;
		name2node.clear();
	}
}

// End of File
