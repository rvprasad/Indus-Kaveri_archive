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

package edu.ksu.cis.indus.staticanalyses.flow.instances.ofa;

import edu.ksu.cis.indus.common.soot.Util;

import edu.ksu.cis.indus.staticanalyses.flow.AbstractExprSwitch;
import edu.ksu.cis.indus.staticanalyses.flow.IFGNodeConnector;
import edu.ksu.cis.indus.staticanalyses.flow.IMethodVariant;
import edu.ksu.cis.indus.staticanalyses.flow.IStmtSwitch;
import edu.ksu.cis.indus.staticanalyses.flow.ITokenProcessingWork;
import edu.ksu.cis.indus.staticanalyses.flow.InvocationVariant;
import edu.ksu.cis.indus.staticanalyses.flow.ValuedVariant;
import edu.ksu.cis.indus.staticanalyses.flow.modes.sensitive.allocation.AllocationContext;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokenFilter;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokenManager;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokens;
import edu.ksu.cis.indus.staticanalyses.tokens.IType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.ArrayType;
import soot.Local;
import soot.SootField;
import soot.Type;
import soot.Value;

import soot.jimple.ArrayRef;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.ThisRef;
import soot.jimple.UnopExpr;
import soot.jimple.VirtualInvokeExpr;

/**
 * The expression visitor used in flow insensitive mode of object flow analysis.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @version $Revision$
 * @param <T> DOCUMENT ME!
 */
class FlowInsensitiveExprSwitch<T extends ITokens<T, Value>>
		extends AbstractExprSwitch<FlowInsensitiveExprSwitch<T>, Value, T, OFAFGNode<T>, Type> {

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(FlowInsensitiveExprSwitch.class);

	/**
	 * The token manager to be used.
	 */
	protected final ITokenManager<T, Value, Type> tokenMgr;

	/**
	 * Creates a new <code>FlowInsensitiveExprSwitch</code> instance.
	 *
	 * @param statementSwitch the statement visitor which uses this object.
	 * @param nodeConnector the connector to be used to connect ast and non-ast flow graph node.
	 * @pre statementSwitch != null and nodeConnector != null
	 */
	public FlowInsensitiveExprSwitch(final IStmtSwitch statementSwitch, final IFGNodeConnector<OFAFGNode<T>> nodeConnector) {
		super(statementSwitch, nodeConnector);

		if (fa != null) {
			tokenMgr = fa.getTokenManager();
		} else {
			tokenMgr = null;
		}
	}

	/**
	 * Processes array access expressions. Current implementation processes the primary and connects a node associated with
	 * the primary to a <code>FGAccessNode</code> which monitors this access expressions for new values in the primary.
	 *
	 * @param e the array access expressions.
	 * @pre e != null
	 */
	@Override public void caseArrayRef(final ArrayRef e) {
		process(e.getBaseBox());

		final OFAFGNode<T> _baseNode = getFlowNode();
		final OFAFGNode<T> _ast = method.getASTNode(e, context);
		MethodVariant.setOutFilterOfBasedOn(_ast, e.getType(), tokenMgr);

		final ITokenProcessingWork<T> _work = new ArrayAccessExprWork<T>(method, context, _ast, connector, tokenMgr
				.getNewTokenSet());
		final OFAFGNode<T> _temp = new FGAccessNode<T>(_work, fa, tokenMgr);
		_baseNode.addSucc(_temp);
		process(e.getIndexBox());
		setFlowNode(_ast);
	}

	/**
	 * Processes the cast expression. Current implementation processes the expression being cast.
	 *
	 * @param e the expression to be processed.
	 * @pre e != null
	 */
	@Override public void caseCastExpr(final CastExpr e) {
		process(e.getOpBox());

		if (Util.isReferenceType(e.getCastType())) {
			// NOTE: We need to filter expressions based on the cast type as casts result in type-conformant values at
			// run-time.
			final OFAFGNode<T> _base = getFlowNode();
			final OFAFGNode<T> _cast = method.getASTNode(e, context);
			MethodVariant.setOutFilterOfBasedOn(_cast, e.getType(), tokenMgr);
			_base.addSucc(_cast);
			setFlowNode(_cast);
		}
	}

	/**
	 * Processes the given exception reference expression. This is required to thread the flow of exception in the system.
	 *
	 * @param e is the caught exception reference.
	 * @pre e != null
	 */
	@Override public void caseCaughtExceptionRef(final CaughtExceptionRef e) {
		final OFAFGNode<T> _node = method.getASTNode(e, context);
		MethodVariant.setOutFilterOfBasedOn(_node, e.getType(), tokenMgr);
		setFlowNode(_node);
	}

	/**
	 * Processes the field expression in a fashion similar to array access expressions.
	 *
	 * @param e the expression to be processed.
	 * @pre e != null
	 */
	@Override public void caseInstanceFieldRef(final InstanceFieldRef e) {
		process(e.getBaseBox());

		final OFAFGNode<T> _baseNode = getFlowNode();
		final OFAFGNode<T> _ast = method.getASTNode(e, context);
		MethodVariant.setOutFilterOfBasedOn(_ast, e.getType(), tokenMgr);

		final ITokenProcessingWork<T> _work = new FieldAccessExprWork<T>(method, context, _ast, connector, tokenMgr
				.getNewTokenSet());
		final FGAccessNode<T> _temp = new FGAccessNode<T>(_work, fa, tokenMgr);
		_baseNode.addSucc(_temp);
		setFlowNode(_ast);
	}

	/**
	 * Processes the embedded expressions.
	 *
	 * @param e the expression to be processed.
	 * @pre e != null
	 */
	@Override public void caseInstanceOfExpr(final InstanceOfExpr e) {
		process(e.getOpBox());
	}

	/**
	 * Processes the embedded expressions.
	 *
	 * @param e the expression to be processed.
	 * @pre e != null
	 */
	@Override public void caseInterfaceInvokeExpr(final InterfaceInvokeExpr e) {
		processInstanceInvokeExpr(e);
	}

	/**
	 * Processes the local expression.
	 *
	 * @param e the expression to be processed.
	 * @pre != null
	 */
	@Override public void caseLocal(final Local e) {
		final OFAFGNode<T> _node = method.getASTNode(e, context);
		MethodVariant.setOutFilterOfBasedOn(_node, e.getType(), tokenMgr);
		setFlowNode(_node);
	}

	/**
	 * Processes the new array expression. This injects a value into the flow graph.
	 *
	 * @param e the expression to be processed.
	 * @pre e != null
	 */
	@Override public void caseNewArrayExpr(final NewArrayExpr e) {
		process(e.getSizeBox());

		Object _temp = null;

		final boolean _flag = context instanceof AllocationContext;

		if (_flag) {
			_temp = ((AllocationContext) context).setAllocationSite(e);
		}

		final OFAFGNode<T> _ast = method.getASTNode(e, context);
		MethodVariant.setOutFilterOfBasedOn(_ast, e.getType(), tokenMgr);
		fa.getArrayVariant((ArrayType) e.getType(), context);
		_ast.injectValue(e);
		setFlowNode(_ast);

		if (_flag) {
			((AllocationContext) context).setAllocationSite(_temp);
		}
	}

	/**
	 * Processes the new expression. This injects a value into the flow graph.
	 *
	 * @param e the expression to be processed.
	 * @pre e != null
	 */
	@Override public void caseNewExpr(final NewExpr e) {
		final OFAFGNode<T> _ast = method.getASTNode(e, context);
		_ast.injectValue(e);
		setFlowNode(_ast);
	}

	/**
	 * Processes the new array expression. This injects values into the flow graph for each dimension for which the size is
	 * specified.
	 *
	 * @param e the expression to be processed.
	 * @pre e != null
	 */
	@Override public void caseNewMultiArrayExpr(final NewMultiArrayExpr e) {
		final ArrayType _arrayType = e.getBaseType();
		final Type _baseType = _arrayType.baseType;

		Object _temp = null;

		final boolean _flag = context instanceof AllocationContext;

		if (_flag) {
			_temp = ((AllocationContext) context).setAllocationSite(e);
		}

		for (int _i = _arrayType.numDimensions, _sizes = e.getSizeCount(); _i > 0 && _sizes > 0; _i--, _sizes--) {
			final ArrayType _aType = ArrayType.v(_baseType, _i);
			final ValuedVariant<OFAFGNode<T>> _array = fa.getArrayVariant(_aType, context);
			process(e.getSizeBox(_sizes - 1));
			_array.getFGNode().injectValue(e);
		}

		if (_flag) {
			((AllocationContext) context).setAllocationSite(_temp);
		}

		final OFAFGNode<T> _ast = method.getASTNode(e, context);
		_ast.injectValue(e);
		setFlowNode(_ast);
	}

	/**
	 * Processes <code>null</code>. This injects a value into the flow graph.
	 *
	 * @param e the expression to be processed.
	 * @pre e != null
	 */
	@Override public void caseNullConstant(final NullConstant e) {
		final OFAFGNode<T> _ast = method.getASTNode(e, context);
		_ast.injectValue(e);
		setFlowNode(_ast);
	}

	/**
	 * Processes parameter reference expressions.
	 *
	 * @param e the expression to be processed.
	 * @pre e != null
	 */
	@Override public void caseParameterRef(final ParameterRef e) {
		setFlowNode(method.queryParameterNode(e.getIndex()));
	}

	/**
	 * Processes the embedded expressions.
	 *
	 * @param e the expression to be processed.
	 */
	@Override public void caseSpecialInvokeExpr(final SpecialInvokeExpr e) {
		processInvokedMethod(e);
	}

	/**
	 * Processes the embedded expressions.
	 *
	 * @param e the expression to be processed.
	 * @pre e != null
	 */
	@Override public void caseStaticFieldRef(final StaticFieldRef e) {
		final SootField _field = e.getField();
		final OFAFGNode<T> _ast = method.getASTNode(e, context);
		MethodVariant.setOutFilterOfBasedOn(_ast, e.getType(), tokenMgr);

		final OFAFGNode<T> _nonast = fa.getFieldVariant(_field).getFGNode();
		connector.connect(_ast, _nonast);
		setFlowNode(_ast);
	}

	/**
	 * Processes the embedded expressions.
	 *
	 * @param e the expression to be processed.
	 * @pre e != null
	 */
	@Override public void caseStaticInvokeExpr(final StaticInvokeExpr e) {
		processInvokedMethod(e);
	}

	/**
	 * Processes a string constant. This injects a value into the flow graph.
	 *
	 * @param e the expression to be processed.
	 * @pre e != null
	 */
	@Override public void caseStringConstant(final StringConstant e) {
		final OFAFGNode<T> _ast = method.getASTNode(e, context);
		_ast.injectValue(e);
		setFlowNode(_ast);
	}

	/**
	 * Processes the <code>this</code> variable. Current implementation returns the node associated with the enclosing
	 * method.
	 *
	 * @param e the expression to be processed.
	 * @pre e != null
	 */
	@Override public void caseThisRef(@SuppressWarnings("unused") final ThisRef e) {
		setFlowNode(method.queryThisNode());
	}

	/**
	 * Processes the embedded expressions.
	 *
	 * @param e the expression to be processed.
	 * @pre e != null
	 */
	@Override public void caseVirtualInvokeExpr(final VirtualInvokeExpr e) {
		processInstanceInvokeExpr(e);
	}

	/**
	 * Processes cases which are not dealt by this visitor methods or delegates to suitable methods depending on the type.
	 *
	 * @param o the expression to be processed.
	 * @pre e != null
	 */
	@Override public void defaultCase(final Object o) {
		final Value _v = (Value) o;

		if (_v instanceof BinopExpr) {
			final BinopExpr _temp = (BinopExpr) _v;
			process(_temp.getOp1Box());
			process(_temp.getOp2Box());
		} else if (_v instanceof UnopExpr) {
			final UnopExpr _temp = (UnopExpr) _v;
			process(_temp.getOpBox());
		} else {
			super.defaultCase(o);
		}
	}

	/**
	 * Returns a new instance of the this class.
	 *
	 * @param o the statement visitor which uses the new instance.
	 * @return the new instance of this class.
	 * @pre o != null and o[0].oclIsKindOf(IStmtSwitch)
	 * @post result != null
	 */
	@Override public FlowInsensitiveExprSwitch<T> getClone(final Object... o) {
		return new FlowInsensitiveExprSwitch<T>((IStmtSwitch) o[0], connector);
	}

	/**
	 * Processes the invoke expressions that require resolution by creating nodes to various data components present at the
	 * call-site and making them available to be connected when new method implementations are plugged in.
	 *
	 * @param e the invoke expression to be processed.
	 * @pre e != null
	 */
	protected void processInstanceInvokeExpr(final InstanceInvokeExpr e) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("BEGIN: processing " + e);
		}
		fa.processClass(e.getMethod().getDeclaringClass());
		process(e.getBaseBox());

		final OFAFGNode<T> _receiverNode = getFlowNode();
		MethodVariant.setOutFilterOfBasedOn(_receiverNode, e.getBase().getType(), tokenMgr);

		for (int _i = 0; _i < e.getArgCount(); _i++) {
			process(e.getArgBox(_i));
		}

		final InvocationVariant<OFAFGNode<T>> _iv = (InvocationVariant) method.getASTVariant(e, context);
		final OFAFGNode<T> _ast = _iv.getFGNode();
		final IType _baseType = tokenMgr.getTypeManager()
				.getTokenTypeForRepType(fa.getClass("java.lang.Throwable").getType());
		final ITokenFilter<T, Value> _baseFilter = tokenMgr.getTypeBasedFilter(_baseType);
		_ast.setOutFilter(_baseFilter);

		if (Util.isReferenceType(e.getMethod().getReturnType())) {
			setFlowNode(_ast);
		} else {
			setFlowNode(null);
		}

		final IType _tokenTypeForRepType = tokenMgr.getTypeManager().getTokenTypeForRepType(e.getBase().getType());
		final ITokenFilter<T, Value> _typeBasedFilter = tokenMgr.getTypeBasedFilter(_tokenTypeForRepType);
		final ITokenProcessingWork<T> _work = new InvokeExprWork<T>(method, context, tokenMgr.getNewTokenSet());
		final FGAccessNode<T> _baseNode = new FGAccessNode<T>(_work, fa, tokenMgr);
		_baseNode.setInFilter(_typeBasedFilter);
		_receiverNode.addSucc(_baseNode);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("END: processed " + e);
		}
	}

	/**
	 * Processes the invoke expressions that do not require resolution by creating nodes to various data components present at
	 * the call-site and making them available to be connected when new method implementations are plugged in.
	 *
	 * @param e the invoke expression to be processed.
	 * @pre e != null
	 */
	private void processInvokedMethod(final InvokeExpr e) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("BEGIN: processing " + e);
		}

		final IMethodVariant<OFAFGNode<T>> _callee = fa.getMethodVariant(e.getMethod(), context);

		if (e instanceof SpecialInvokeExpr) {
			final SpecialInvokeExpr _expr = (SpecialInvokeExpr) e;
			final OFAFGNode<T> _thisNode = _callee.queryThisNode();
			process(_expr.getBaseBox());

			final OFAFGNode<T> _thisArgNode = getFlowNode();
			MethodVariant.setOutFilterOfBasedOn(_thisArgNode, _expr.getBase().getType(), tokenMgr);

			_thisArgNode.addSucc(_thisNode);
		}

		for (int _i = 0; _i < e.getArgCount(); _i++) {
			if (Util.isReferenceType(e.getArg(_i).getType())) {
				process(e.getArgBox(_i));

				final OFAFGNode<T> _argNode = getFlowNode();
				MethodVariant.setOutFilterOfBasedOn(_argNode, e.getArg(_i).getType(), tokenMgr);

				_argNode.addSucc(_callee.queryParameterNode(_i));
			}
		}

		final InvocationVariant<OFAFGNode<T>> _iv = (InvocationVariant) method.getASTVariant(e, context);
		final OFAFGNode<T> _throwNode = _iv.getThrowNode();
		final IType _baseType = tokenMgr.getTypeManager()
				.getTokenTypeForRepType(fa.getClass("java.lang.Throwable").getType());
		final ITokenFilter<T, Value> _baseFilter = tokenMgr.getTypeBasedFilter(_baseType);
		_throwNode.setOutFilter(_baseFilter);
		_callee.queryThrownNode().addSucc(_throwNode);

		if (Util.isReferenceType(e.getMethod().getReturnType())) {
			final OFAFGNode<T> _ast = _iv.getFGNode();
			MethodVariant.setOutFilterOfBasedOn(_ast, e.getType(), tokenMgr);
			_callee.queryReturnNode().addSucc(_ast);
			setFlowNode(_ast);
		} else {
			setFlowNode(null);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("END: processed " + e);
		}
	}
}

// End of File
