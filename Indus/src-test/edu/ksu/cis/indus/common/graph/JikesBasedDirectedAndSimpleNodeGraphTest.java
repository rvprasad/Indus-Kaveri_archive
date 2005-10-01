
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

import java.util.Collection;
import java.util.HashSet;


/**
 * This class tests directed graph and simple node graph implementations with a different graph instance.  All the local
 * tests are overloaded suitably.
 * 
 * <p>
 * This represents the CFG as generated by <b>jikes</b> for the following snippet.
 * </p>
 * <pre>
 *  public static void main(String[] s) {
 *       int i = 0;
 *       do {
 *           while(i > 0) {
 *               i--;
 *           };
 *           i++;
 *       } while (i < 10);
 *       System.out.println("Hi");
 *   }
 * </pre>
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class JikesBasedDirectedAndSimpleNodeGraphTest
  extends SimpleNodeGraphTest {
	/**
	 * @see edu.ksu.cis.indus.common.graph.AbstractDirectedGraphTest#testAddEdgeFromTo()
	 */
	public void localtestAddEdgeFromTo() {
		// we do nothing here as we do not want to change the graph.
	}

	/**
	 * @see edu.ksu.cis.indus.common.graph.SimpleNodeGraphTest#testGetNodesInPathBetween()
	 */
	public void testGetNodesInPathBetween() {
		final Collection _t = new HashSet();
		_t.add(sng.getNode("b"));
		_t.add(sng.getNode("d"));

		final Collection _nodes = dg.getNodesOnPathBetween(_t);
		_t.add(sng.getNode("c"));
		assertTrue(_nodes.containsAll(_t));
	}

	/**
	 * @see edu.ksu.cis.indus.common.graph.SimpleNodeGraphTest#testGetTails()
	 */
	public void testGetTails() {
		testlocalGetSinks();
	}

	/**
	 * @see edu.ksu.cis.indus.common.graph.SimpleNodeGraphTest#testlocalGetSinks()
	 */
	public void testlocalGetSinks() {
		assertFalse(dg.getSinks().isEmpty());
		assertTrue(dg.getSinks().contains(name2node.get("f")));
	}

	/**
	 * We construct a graph with cycles enclosed in cycles.
	 *
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp()
	  throws Exception {
		final SimpleNodeGraph<String> _sng = new SimpleNodeGraph<String>();
		name2node.put("a", _sng.getNode("a"));
		name2node.put("b", _sng.getNode("b"));
		name2node.put("c", _sng.getNode("c"));
		name2node.put("d", _sng.getNode("d"));
		name2node.put("e", _sng.getNode("e"));
		name2node.put("f", _sng.getNode("f"));
		_sng.addEdgeFromTo( name2node.get("a"),  name2node.get("b"));
		_sng.addEdgeFromTo( name2node.get("b"),  name2node.get("c"));
		_sng.addEdgeFromTo( name2node.get("c"),  name2node.get("d"));
		_sng.addEdgeFromTo( name2node.get("c"),  name2node.get("e"));
		_sng.addEdgeFromTo( name2node.get("e"),  name2node.get("f"));
		// add loop edges
		_sng.addEdgeFromTo( name2node.get("d"),  name2node.get("c"));
		_sng.addEdgeFromTo( name2node.get("e"),  name2node.get("b"));
		setSNG(_sng);

		numberOfCycles = 2;
	}

	/**
	 * @see edu.ksu.cis.indus.common.graph.AbstractDirectedGraphTest#localtestIsAncestorOf()
	 */
	protected void localtestIsAncestorOf() {
		assertTrue(sng.isAncestorOf( name2node.get("a"),  name2node.get("a")));
		assertTrue(sng.isAncestorOf( name2node.get("a"),  name2node.get("f")));
		assertFalse(sng.isAncestorOf( name2node.get("e"),  name2node.get("b")));
	}

	/**
	 * @see edu.ksu.cis.indus.common.graph.AbstractDirectedGraphTest#localtestIsReachable()
	 */
	protected void localtestIsReachable() {
		assertTrue(sng.isReachable( name2node.get("b"),  name2node.get("d"), false));
		assertTrue(sng.isReachable( name2node.get("b"),  name2node.get("d"), true));
		assertTrue(sng.isReachable( name2node.get("a"),  name2node.get("f"), true));
		assertTrue(sng.isReachable( name2node.get("f"),  name2node.get("a"), false));
	}
}

// End of File
