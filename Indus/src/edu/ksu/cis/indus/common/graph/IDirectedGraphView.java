
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


/**
 * This interface is intended to be used to view some data as a directed graph without actually constructing the graph.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$
 */
public interface IDirectedGraphView {
	/**
	 * This is a marker interface for the node of the graph being navigated.
	 *
	 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
	 * @author $Author$
	 * @version $Revision$
	 */
	public interface INode {
	}

	/**
	 * Retrieves the predecessors of the given node.
	 *
	 * @param node of interest.
	 *
	 * @return the collection of predecessors of the given node.
	 *
	 * @post result->forall(o | o.oclIsKindOf(INode))
	 * @post result->forall(o | getSuccsOf(o)->includes(node))
	 */
	Collection getPredsOf(final INode node);

	/**
	 * Retrieves the successors of the given node.
	 *
	 * @param node of interest.
	 *
	 * @return the collection of successors of the given node.
	 *
	 * @post result->forall(o | o.oclIsKindOf(INode))
	 * @post result->forall(o | getPredsOf(o)->includes(node))
	 */
	Collection getSuccsOf(final INode node);
}

// End of File