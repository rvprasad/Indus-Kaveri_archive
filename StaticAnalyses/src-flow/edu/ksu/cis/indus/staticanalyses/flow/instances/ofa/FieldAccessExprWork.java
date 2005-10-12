
/*
 * Indus, a toolkit to customize and adapt Java programs.
 * Copyright (c) 2002, 2003, 2004, 2005 SAnToS Laboratory, Kansas State University
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

package edu.ksu.cis.indus.staticanalyses.flow.instances.ofa;

import edu.ksu.cis.indus.processing.Context;

import edu.ksu.cis.indus.staticanalyses.flow.IFGNode;
import edu.ksu.cis.indus.staticanalyses.flow.IFGNodeConnector;
import edu.ksu.cis.indus.staticanalyses.flow.IMethodVariant;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokens;

import soot.SootField;

import soot.jimple.FieldRef;


/**
 * This class encapsulates the logic to instrument the flow of values corresponding to fields.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @version $Revision$
 */
class FieldAccessExprWork<N extends IFGNode<N, ?>>
  extends AbstractMemberDataAccessExprWork<N> {
	/**
	 * Creates a new <code>FieldAccessExprWork</code> instance.
	 *
	 * @param callerMethod the method in which the access occurs.
	 * @param accessContext the context in which the access occurs.
	 * @param accessNode the flow graph node associated with the access expression.
	 * @param connectorToBeUsed the connector to use to connect the ast node to the non-ast node.
	 * @param tokenSet used to store the tokens that trigger the execution of this work peice.
	 *
	 * @pre callerMethod != null and accessContext != null and accessNode != null and     connectorToBeUsed != null and
	 * 		tokenSet != null
	 */
	public FieldAccessExprWork(final IMethodVariant<N, ?, ?, ?> callerMethod, final Context accessContext, final N accessNode,
		final IFGNodeConnector<N> connectorToBeUsed, final ITokens tokenSet) {
		super(callerMethod, accessContext, accessNode, connectorToBeUsed, tokenSet);
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.flow.instances.ofa.AbstractMemberDataAccessExprWork#getFGNodeForMemberData()
	 */
	@Override protected N getFGNodeForMemberData() {
		final SootField _sf = ((FieldRef) accessExprBox.getValue()).getField();
		return caller.getFA().getFieldVariant(_sf, context).getFGNode();
	}
}

// End of File
