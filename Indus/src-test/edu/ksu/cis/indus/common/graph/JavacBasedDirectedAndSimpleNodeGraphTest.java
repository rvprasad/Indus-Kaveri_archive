
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

import edu.ksu.cis.indus.common.graph.IDirectedGraph.INode;
import edu.ksu.cis.indus.common.graph.SimpleNodeGraph.SimpleNode;


/**
 * This class tests directed graph and simple node graph implementations with a different graph instance.  All the local
 * tests are overloaded suitably.
 * 
 * <p>
 * This represents the CFG as generated by <b>javac</b> for the following snippet.
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
public class JavacBasedDirectedAndSimpleNodeGraphTest
  extends JikesBasedDirectedAndSimpleNodeGraphTest {
	/**
	 * @see edu.ksu.cis.indus.common.graph.SimpleNodeGraphTest#testlocalGetSinks()
	 */
	public void testlocalGetSinks() {
		assertFalse(dg.getSinks().isEmpty());
		assertTrue(dg.getSinks().contains(name2node.get("e")));
	}

	/**
	 * @see JikesBasedDirectedAndSimpleNodeGraphTest#setUp()
	 */
	protected void setUp()
	  throws Exception {
		final SimpleNodeGraph _sng = new SimpleNodeGraph();
		name2node.put("a", _sng.getNode("a"));
		name2node.put("b", _sng.getNode("b"));
		name2node.put("c", _sng.getNode("c"));
		name2node.put("d", _sng.getNode("d"));
		name2node.put("e", _sng.getNode("e"));

		_sng.addEdgeFromTo((SimpleNode) name2node.get("a"), (SimpleNode) name2node.get("b"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("b"), (SimpleNode) name2node.get("c"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("b"), (SimpleNode) name2node.get("d"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("d"), (SimpleNode) name2node.get("e"));
		// add loop edges
		_sng.addEdgeFromTo((SimpleNode) name2node.get("c"), (SimpleNode) name2node.get("b"));
		_sng.addEdgeFromTo((SimpleNode) name2node.get("d"), (SimpleNode) name2node.get("b"));
		setSNG(_sng);

		numberOfCycles = 2;
	}

	/**
	 * @see edu.ksu.cis.indus.common.graph.AbstractDirectedGraphTest#localtestIsAncestorOf()
	 */
	protected void localtestIsAncestorOf() {
		assertTrue(sng.isAncestorOf((INode) name2node.get("a"), (INode) name2node.get("a")));
		assertTrue(sng.isAncestorOf((INode) name2node.get("a"), (INode) name2node.get("e")));
		assertFalse(sng.isAncestorOf((INode) name2node.get("e"), (INode) name2node.get("b")));
	}

	/**
	 * @see edu.ksu.cis.indus.common.graph.AbstractDirectedGraphTest#localtestIsReachable()
	 */
	protected void localtestIsReachable() {
		assertTrue(sng.isReachable((INode) name2node.get("a"), (INode) name2node.get("e"), true));
		assertTrue(sng.isReachable((INode) name2node.get("e"), (INode) name2node.get("a"), false));
		assertTrue(sng.isReachable((INode) name2node.get("c"), (INode) name2node.get("e"), true));
		assertTrue(sng.isReachable((INode) name2node.get("d"), (INode) name2node.get("c"), true));
	}
}

// End of File
