
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

import soot.SootClass;
import soot.SootMethod;
import soot.Value;

import soot.jimple.InvokeExpr;


/**
 * This is the interface to a method variant instance in the flow analysis framework.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$
 */
public interface IMethodVariant
  extends IVariant {
	/**
	 * Returns the flow graph node associated with the given AST node in the context defined by <code>this.context</code>.
	 *
	 * @param v the AST node whose associted flow graph node is to be returned.
	 *
	 * @return the flow graph node associated with <code>v</code> in the context <code>this.context</code>.
	 *
	 * @pre v != null
	 */
	IFGNode getASTNode(final Value v);

	/**
	 * Returns the flow graph node associated with the given AST node in the given context.  Creates a new one if none
	 * exists.
	 *
	 * @param v the AST node whose associted flow graph node is to be returned.
	 * @param c the context in which the flow graph node was associated with <code>v</code>.
	 *
	 * @return the flow graph node associated with <code>v</code> in context <code>c</code>.
	 *
	 * @pre v != null and c != null
	 */
	IFGNode getASTNode(final Value v, final Context c);

	/**
	 * Returns the variant associated with the given AST node in the given context.  Creates a new one if none exists.
	 *
	 * @param v the AST node whose associated variant is to be returned.
	 * @param ctxt the context in which the variant was associated with <code>v</code>.
	 *
	 * @return the variant associated with <code>v</code> in the context <code>c</code>.
	 *
	 * @pre v != null and ctxt != null
	 */
	ValuedVariant getASTVariant(final Value v, final Context ctxt);

	/**
	 * Retrieves the context used by this method variant.
	 *
	 * @return the context used by this method.
	 *
	 * @post result != null
	 */
	Context getContext();

	/**
	 * Retrieves the flow analysis instance used by this method variant.
	 *
	 * @return the flow analysis instance used by this method.
	 *
	 * @post result != null
	 */
	FA getFA();

	/**
	 * Retrieves the method used by this method variant.
	 *
	 * @return the method used by this method.
	 *
	 * @post result != null
	 */
	SootMethod getMethod();

	/**
	 * Same as <code>getASTNode</code>, except <code>null</code> is returned if none exists.
	 *
	 * @param v the AST node whose associted variant is to be returned.
	 *
	 * @return the flow graph node associated with <code>v</code> in context <code>c</code>.  If none exists,
	 * 		   <code>null</code> is returned.
	 *
	 * @pre v != null
	 */
	IFGNode queryASTNode(final Value v);

	/**
	 * Same as <code>getASTNode</code>, except <code>null</code> is returned if none exists.
	 *
	 * @param v the AST node whose associated variant is to be returned.
	 * @param c the context in which the variant was associated with <code>v</code>.
	 *
	 * @return the flow graph node associated with <code>v</code> in context <code>c</code>.  If none exists,
	 * 		   <code>null</code> is returned.
	 *
	 * @pre v != null and c != null
	 */
	IFGNode queryASTNode(final Value v, final Context c);

	/**
	 * Same as <code>getASTVariant</code>, except <code>null</code> is returned if none exists.
	 *
	 * @param v the AST node whose associated variant is to be returned.
	 * @param c a <code>Context</code> value
	 *
	 * @return the variant associated with <code>v</code> in the context <code>c</code>.  If none exists, <code>null</code>
	 * 		   is returned.
	 *
	 * @pre v != null and c != null
	 */
	ValuedVariant queryASTVariant(final Value v, final Context c);

	/**
	 * Returns the flow graph node associated with the given parameter.
	 *
	 * @param index the index of the parameter in the parameter list of the associated method.
	 *
	 * @return the flow graph node associated with the <code>index</code>th parameter in the parameter list of the associated
	 * 		   method.  It returns <code>null</code> if the method has no parameters or if mentioned parameter is of non-ref
	 * 		   type.
	 */
	IFGNode queryParameterNode(final int index);

	/**
	 * Returns the flow graph node that represents an abstract single return point of the associated method.
	 *
	 * @return the flow graph node that represents an abstract single return point of the associated method.
	 * 		   <code>null</code> if the corresponding method does not return a value or if it returns non-ref typed value.
	 */
	IFGNode queryReturnNode();

	/**
	 * Returns the flow graph node associated with the <code>this</code> variable of the associated method.
	 *
	 * @return Returns the flow graph node associated with the <code>this</code> variable of the associated method.
	 * 		   <code>null</code> if the corresponding method is <code>static</code>.
	 */
	IFGNode queryThisNode();

	/**
	 * Returns the flow graph node associated with <code>exception</code> class at invoke expression <code>e</code>.
	 *
	 * @param e is the method invoke expression.
	 * @param exception is the class of the exception thrown at <code>e</code>.
	 *
	 * @return the node that captures values associated with the <code>exception</code> class at <code>e</code>.
	 *
	 * @pre e != null and exception != null
	 */
	IFGNode queryThrowNode(final InvokeExpr e, final SootClass exception);

	/**
	 * Returns the flow graph node associated with <code>exception</code> class at invoke expression <code>e</code>.
	 *
	 * @param e is the method invoke expression.
	 * @param exception is the class of the exception thrown at <code>e</code>.
	 * @param c is the context in which the node is requested.
	 *
	 * @return the node that captures values associated with the <code>exception</code> class at <code>e</code>.
	 *
	 * @pre e != null and exception != null and c != null
	 */
	IFGNode queryThrowNode(final InvokeExpr e, final SootClass exception, final Context c);
}

// End of File