
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
 * This interface is used to build graphs.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$
 */
public interface IGraphBuilder {
	/**
	 * Retrieves the built graph.
	 *
	 * @return the build graph.
	 */
	IObjectDirectedGraph getBuiltGraph();

	/**
	 * Adds an edge from the node representing <code>src</code> to the nodes representing the nodes in <code>nodes</code>.
	 *
	 * @param src is the node in the originating graph.
	 * @param nodes are the nodes in the originating graph.
	 *
	 * @pre src != null and nodes != null and nodes.oclIsKindOf(Collection(INode))
	 */
	void addEdgeFromTo(INode src, Collection nodes);

	/**
	 * Adds an edge from the nodes representing the nodes in <code>nodes</code> to the node representing <code>dest</code>.
	 *
	 * @param nodes are the nodes in the originating graph.
	 * @param dest is the node in the originating graph.
	 *
	 * @pre dest != null and nodes != null and nodes.oclIsKindOf(Collection(INode))
	 */
	void addEdgeFromTo(Collection nodes, INode dest);

	/**
	 * Create the graph to be build.
	 */
	void createGraph();

	/**
	 * Creates a node representing the given object.
	 *
	 * @param element to be represented by the node.
	 */
	void createNode(Object element);

	/**
	 * Finish up the built graph.
	 */
	void finishBuilding();
}

// End of File