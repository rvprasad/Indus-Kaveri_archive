
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

package edu.ksu.cis.indus.common.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This is a simple concrete implementation of <code>DirectedGraph</code> interface.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$
 */
public final class SimpleNodeGraph
  extends AbstractMutableDirectedGraph {
	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Log LOGGER = LogFactory.getLog(SimpleNodeGraph.class);

	/**
	 * The sequence of nodes in this graph.  They are stored in the order that the nodes are created.
	 *
	 * @invariant nodes.oclIsTypeOf(Sequence(SimpleNode))
	 */
	private List nodes = new ArrayList();

	/**
	 * This maps objects to their representative nodes.
	 *
	 * @invariant object2nodes.oclIsTypeOf(Map(Object, SimpleNode))
	 */
	private Map object2nodes = new HashMap();

	/**
	 * This is a simple concrete implementation of <code>INode</code> interface.
	 *
	 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
	 * @author $Author$
	 * @version $Revision$
	 */
	public final class SimpleNode
	  extends AbstractMutableDirectedGraph.AbstractMutableNode {
		/**
		 * The object being represetned by this node.
		 */
		private final Object object;

		/**
		 * Creates a new SimpleNode object.
		 *
		 * @param o is the object to be represented by this node.
		 */
		SimpleNode(final Object o) {
			super(new HashSet(), new HashSet());
			this.object = o;
		}

		/**
		 * Retrieves the associated object.
		 *
		 * @return the associated object.
		 */
		public Object getObject() {
			return object;
		}

		/**
		 * Returns the stringized representation of this object.
		 *
		 * @return stringized representation.
		 *
		 * @post result != null
		 */
		public String toString() {
			return object + "";
		}
	}

	/**
	 * Returns a node that represents <code>o</code> in this graph.  If no such node exists, then a new node is created.
	 *
	 * @param o is the object being represented by a node in this graph.
	 *
	 * @return the node representing <code>o</code>.
	 *
	 * @throws NullPointerException when the given object is <code>null</code>.
	 *
	 * @pre o != null
	 * @post object2nodes$pre.get(o) == null implies inclusion
	 * @post inclusion: nodes->includes(result) and heads->includes(result) and tails->includes(result) and
	 * 		 object2nodes.get(o) == result
	 * @post result != null
	 */
	public INode getNode(final Object o) {
		if (o == null) {
            ///CLOVER:OFF
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("object to be represented cannot be null.");
			}
            ///CLOVER:ON
			throw new NullPointerException("object to be represented cannot be null.");
		}

		INode _result = (INode) object2nodes.get(o);

		if (_result == null) {
			_result = new SimpleNode(o);
			object2nodes.put(o, _result);
			nodes.add(_result);
			heads.add(_result);
			tails.add(_result);
			hasSpanningForest = false;
		}
		return _result;
	}

	/**
	 * @see edu.ksu.cis.indus.common.graph.DirectedGraph#getNodes()
	 */
	public List getNodes() {
		return Collections.unmodifiableList(nodes);
	}

	/**
	 * @see AbstractMutableDirectedGraph#containsNodes(edu.ksu.cis.indus.common.graph.INode)
	 */
	protected boolean containsNodes(final INode node) {
		return nodes.contains(node);
	}
}

/*
   ChangeLog:
   $Log$
   Revision 1.2  2003/12/13 02:28:53  venku
   - Refactoring, documentation, coding convention, and
     formatting.

   Revision 1.1  2003/12/09 04:22:03  venku
   - refactoring.  Separated classes into separate packages.
   - ripple effect.
   Revision 1.1  2003/12/08 12:15:48  venku
   - moved support package from StaticAnalyses to Indus project.
   - ripple effect.
   - Enabled call graph xmlization.
   Revision 1.8  2003/12/02 09:42:37  venku
   - well well well. coding convention and formatting changed
     as a result of embracing checkstyle 3.2
   Revision 1.7  2003/11/06 05:04:02  venku
   - renamed WorkBag to IWorkBag and the ripple effect.
   Revision 1.6  2003/09/28 03:16:20  venku
   - I don't know.  cvs indicates that there are no differences,
     but yet says it is out of sync.
   Revision 1.5  2003/09/11 01:50:05  venku
   - any change to the graph did not invalidate the spanning tree. FIXED.
   Revision 1.4  2003/08/24 08:13:11  venku
   Major refactoring.
    - The methods to modify the graphs were exposed.
    - The above anamoly was fixed by supporting a new class AbstractMutableDirectedGraph.
    - Each Mutable graph extends this graph and exposes itself via
      suitable interface to restrict access.
    - Ripple effect of the above changes.
   Revision 1.3  2003/08/11 06:40:54  venku
   Changed format of change log accumulation at the end of the file.
   Spruced up Documentation and Specification.
   Formatted source.
   Revision 1.2  2003/08/11 04:20:19  venku
   - Pair and Triple were changed to work in optimized and unoptimized mode.
   - Ripple effect of the previous change.
   - Documentation and specification of other classes.
   Revision 1.1  2003/08/07 06:42:16  venku
   Major:
    - Moved the package under indus umbrella.
    - Renamed isEmpty() to hasWork() in IWorkBag.
   Revision 1.5  2003/05/22 22:18:31  venku
   All the interfaces were renamed to start with an "I".
   Optimizing changes related Strings were made.
 */
