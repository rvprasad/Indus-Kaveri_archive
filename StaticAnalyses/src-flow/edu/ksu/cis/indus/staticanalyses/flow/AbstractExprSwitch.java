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

package edu.ksu.cis.indus.staticanalyses.flow;

import edu.ksu.cis.indus.processing.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Value;
import soot.ValueBox;

import soot.jimple.AbstractJimpleValueSwitch;

/**
 * The expression visitor class. This class provides the default method implementations for all the expressions that need to
 * be dealt at Jimple level in Bandera framework. The class is tagged as <code>abstract</code> to force the users to extend
 * the class as required. It patches the inheritance hierarchy to inject the new constructs declared in
 * <code>BanderaExprSwitch</code> into the visitor provided in <code>AbstractJimpleValueSwitch</code>.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$
 * @param <E> DOCUMENT ME!
 * @param <N> DOCUMENT ME!
 */
public abstract class AbstractExprSwitch<E extends AbstractExprSwitch<E, N>, N extends IFGNode<N, ?>>
		extends AbstractJimpleValueSwitch
		implements IExprSwitch<E, N> {

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExprSwitch.class);

	/**
	 * This visitor works in the context given by <code>context</code>.
	 */
	protected final Context context;

	/**
	 * The instance of the underlying flow analysis framework.
	 */
	protected final FA<N, ?, ?, ?, ?, ?, ?, ?, ?, ?> fa;

	/**
	 * DOCUMENT ME!
	 */
	private N resultFlowNode;

	/**
	 * The object used to connect flow graph nodes corresponding to AST and non-AST entities. This provides the flexibility to
	 * use the same implementation of the visitor with different connectors to process LHS and RHS entities.
	 */
	protected final IFGNodeConnector<N> connector;

	/**
	 * This visitor is used to visit the expressions in the <code>method</code> variant.
	 */
	protected final IMethodVariant<N, ?, ?, ?> method;

	/**
	 * This visitor is used by <code>stmt</code> to walk the embedded expressions.
	 */
	protected final IStmtSwitch stmtSwitch;

	/**
	 * Creates a new <code>AbstractExprSwitch</code> instance. In non-prototype mode, all of the fields (declared in this
	 * class) will be non-null after returning from the constructor.
	 * 
	 * @param stmtVisitor the statement visitor which shall use this expression visitor.
	 * @param connectorToUse the connector to be used by this expression visitor to connect flow graph nodes corresponding to
	 *            AST and non-AST entities.
	 * @pre connectorToUse != null && stmtVisitor.oclIsKindOf(edu.ksu.cis.indus.staticanalyses.flow.AbstractStmtSwitch)
	 */
	protected AbstractExprSwitch(final IStmtSwitch stmtVisitor, final IFGNodeConnector<N> connectorToUse) {
		this.stmtSwitch = stmtVisitor;
		this.connector = connectorToUse;

		if (stmtSwitch != null) {
			context = ((AbstractStmtSwitch) stmtSwitch).context;
			method = ((AbstractStmtSwitch) stmtSwitch).method;
			fa = ((AbstractStmtSwitch) stmtSwitch).method.getFA();
		} else {
			context = null;
			method = null;
			fa = null;
		}
	}

	/**
	 * This method will throw <code>UnsupportedOperationException</code>.
	 * 
	 * @return (This method raises an exception.)
	 * @throws UnsupportedOperationException as this method is not supported.
	 */
	public E getClone(final Object... o) {
		throw new UnsupportedOperationException("Parameterless prototype method is not supported.");
	}

	/**
	 * Provides the default implementation when any expression is not handled by the visitor. It sets the flow node associated
	 * with the AST as the result.
	 * 
	 * @param o the expression which is not handled by the visitor.
	 */
	@Override public void defaultCase(final Object o) {
		setResult(method.getASTNode((Value) o));

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(o + "(" + o.getClass() + ") is not handled.");
		}
	}

	/**
	 * Processes the expression at the given program point, <code>v</code>.
	 * 
	 * @param v the program point at which the to-be-processed expression occurs.
	 * @pre v != null
	 */
	public void process(final ValueBox v) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Started to process expression: " + v.getValue());
		}

		final ValueBox _temp = context.setProgramPoint(v);
		v.getValue().apply(this);
		context.setProgramPoint(_temp);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Finished processing expression: " + v.getValue() + "\n" + getResult());
		}
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.flow.IExprSwitch#getFlowNode()
	 */
	public N getFlowNode() {
		return resultFlowNode;
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.flow.IExprSwitch#setFlowNode(N)
	 */
	public void setFlowNode(final N node) {
		resultFlowNode = node;
	}
}

// End of File
