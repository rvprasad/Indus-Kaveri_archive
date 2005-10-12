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

import edu.ksu.cis.indus.staticanalyses.flow.AbstractMethodVariant;
import edu.ksu.cis.indus.staticanalyses.flow.FA;
import edu.ksu.cis.indus.staticanalyses.flow.IFGNode;
import edu.ksu.cis.indus.staticanalyses.flow.IVariantManager;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokenFilter;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokenManager;
import edu.ksu.cis.indus.staticanalyses.tokens.IType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.Trap;
import soot.TrapManager;
import soot.Type;

import soot.jimple.CaughtExceptionRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;

/**
 * The variant that represents a method implementation. It maintains variant specific information about local variables and
 * the AST nodes in associated method. It also maintains information about the parameters, this variable, and return values,
 * if any are present.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @version $Revision$ $Name$
 */
class MethodVariant
		extends AbstractMethodVariant<OFAFGNode, FlowInsensitiveExprSwitch, FlowInsensitiveExprSwitch, StmtSwitch> {

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodVariant.class);

	/**
	 * Creates a new <code>MethodVariant</code> instance. This will not process the statements of this method. That is
	 * accomplished via call to <code>process()</code>. This will also mark the field with the flow analysis tag.
	 * 
	 * @param sm the method represented by this variant. This parameter cannot be <code>null</code>.
	 * @param astVariantManager the manager of flow graph nodes corresponding to the AST nodes of<code>sm</code>. This
	 *            parameter cannot be <code>null</code>.
	 * @param theFA the instance of <code>FA</code> which was responsible for the creation of this variant. This parameter
	 *            cannot be <code>null</code>.
	 * @pre sm != null and astvm != null and theFA != null
	 */
	protected MethodVariant(final SootMethod sm, final IVariantManager astVariantManager,
			final FA<OFAFGNode, ?, ?, ?, ?, FlowInsensitiveExprSwitch, ?, FlowInsensitiveExprSwitch, StmtSwitch, ?> theFA) {
		super(sm, astVariantManager, theFA);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("BEGIN: preprocessing of " + sm);
		}

		/*
		 * NOTE: This is required to filter out values which are descendents of a higher common type but which are
		 * incompatible. An example is all objects entering run() site will have a run() method defined. However, it is false
		 * to assume that all such objects can be considered as receivers for all run() implementations plugged into the run()
		 * site.
		 */
		final ITokenManager _tokenMgr = fa.getTokenManager();

		if (thisVar != null) {
			final RefType _sootType = sm.getDeclaringClass().getType();
			final IType _tokenTypeForRepType = _tokenMgr.getTypeManager().getTokenTypeForRepType(_sootType);
			final ITokenFilter _typeBasedFilter = _tokenMgr.getTypeBasedFilter(_tokenTypeForRepType);
			thisVar.setInFilter(_typeBasedFilter);
			thisVar.setOutFilter(_typeBasedFilter);
		}

		// We also want to use retrieve acceptable values from other interfacial data entities.
		if (returnVar != null) {
			setOutFilterOfBasedOn(returnVar, sm.getReturnType(), _tokenMgr);
		}

		for (int _i = parameters.length - 1; _i >= 0; _i--) {
			final OFAFGNode _pNode = parameters[_i];

			if (_pNode != null) {
				setOutFilterOfBasedOn(_pNode, sm.getParameterType(_i), _tokenMgr);
			}
		}

		setOutFilterOfBasedOn(thrownNode, this.fa.getClass("java.lang.Throwable").getType(), _tokenMgr);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("END: preprocessed " + sm);
		}
	}

	/**
	 * Sets the out filter based on the given type for the given node.
	 * 
	 * @param node of interest.
	 * @param type for the filter.
	 * @param tokenMgr used in the creation of the type-based filter.
	 * @pre node != null and type != null and tokenMgr != null
	 */
	static void setOutFilterOfBasedOn(final OFAFGNode node, final Type type, final ITokenManager tokenMgr) {
		if (node != null) {
			final IType _baseType = tokenMgr.getTypeManager().getTokenTypeForRepType(type);
			final ITokenFilter _baseFilter = tokenMgr.getTypeBasedFilter(_baseType);
			node.setOutFilter(_baseFilter);
		}
	}

	/**
	 * Processes the body of the method implementation associated with this variant.
	 */
	public void process() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("BEGIN: processing of " + method);
		}

		JimpleBody _jb = null;

		// We assume the user has closed the system.
		if (method.isConcrete()) {
			_jb = (JimpleBody) method.retrieveActiveBody();

			final List<Stmt> _stmtList = new ArrayList<Stmt>(_jb.getUnits());

			for (final Iterator<Stmt> _i = _stmtList.iterator(); _i.hasNext();) {
				stmt.process(_i.next());
			}

			processBody(_jb, _stmtList);
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(method + " is not a concrete method. Hence, it's body could not be retrieved.");
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("END: processing of " + method);
		}
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.flow.AbstractMethodVariant#shouldConsider(soot.Type)
	 */
	@Override protected boolean shouldConsider(final Type type) {
		return Util.isReferenceType(type);
	}

	/**
	 * Connects the nodes corresponding the exceptions thrown (throw and method invocations) in the body to the nodd
	 * corresponding to the expression thrown by the method.
	 * 
	 * @param body of the method.
	 * @pre body != null
	 */
	private void connectThrowNodesToThrownNode(final JimpleBody body) {
		final Collection<Stmt> _units = body.getUnits();
		final Iterator<Stmt> _j = _units.iterator();
		final int _jEnd = _units.size();

		for (int _jIndex = 0; _jIndex < _jEnd; _jIndex++) {
			final Stmt _stmt = _j.next();
			context.setStmt(_stmt);

			if (_stmt instanceof ThrowStmt) {
				final Collection _t = TrapManager.getTrapsAt(_stmt, body);

				if (_t.isEmpty()) {
					final ThrowStmt _throwStmt = (ThrowStmt) _stmt;
					context.setProgramPoint(_throwStmt.getOpBox());
					queryASTNode(_throwStmt.getOp(), context).addSucc(thrownNode);
				}
			} else if (_stmt.containsInvokeExpr()) {
				context.setProgramPoint(_stmt.getInvokeExprBox());
				queryThrowNode(_stmt.getInvokeExpr(), context).addSucc(thrownNode);
			}
		}
	}

	/**
	 * Process the body.
	 * 
	 * @param body to be processed.
	 * @param stmtList is the list of statements that make up the body.
	 * @pre body != null and stmtList != null
	 */
	private void processBody(final JimpleBody body, final List<Stmt> stmtList) {
		final Collection<Stmt> _caught = new HashSet<Stmt>();
		boolean _flag = false;
		InvokeExpr _expr = null;

		for (final Iterator<Trap> _i = body.getTraps().iterator(); _i.hasNext();) {
			final Trap _trap = _i.next();
			final Stmt _begin = (Stmt) _trap.getBeginUnit();
			final Stmt _end = (Stmt) _trap.getEndUnit();

			// we assume that the first statement in the handling block will be the identity statement that retrieves the
			// caught expression.
			final CaughtExceptionRef _catchRef = (CaughtExceptionRef) ((IdentityStmt) _trap.getHandlerUnit()).getRightOp();
			final SootClass _exception = _trap.getException();

			final int _k = stmtList.indexOf(_end);

			for (int _j = stmtList.indexOf(_begin); _j < _k; _j++) {
				final Stmt _tmp = stmtList.get(_j);

				if (_tmp instanceof ThrowStmt) {
					final ThrowStmt _ts = (ThrowStmt) _tmp;

					if (!_caught.contains(_ts)) {
						final SootClass _scTemp = fa.getClass(((RefType) _ts.getOp().getType()).getClassName());

						if (Util.isDescendentOf(_scTemp, _exception)) {
							context.setStmt(_ts);

							final IFGNode _throwNode = getASTNode(_ts.getOp(), context);
							_throwNode.addSucc(getASTNode(_catchRef));
							_caught.add(_ts);
						}
					}
				} else if (_tmp.containsInvokeExpr()) {
					_expr = _tmp.getInvokeExpr();
					_flag = true;
				}

				if (_flag) {
					_flag = false;

					if (!_caught.contains(_tmp)) {
						context.setStmt(_tmp);

						final OFAFGNode _tempNode = queryThrowNode(_expr, context);

						if (_tempNode != null) {
							_tempNode.addSucc(getASTNode(_catchRef));
						}
					}
				}
			}
		}

		connectThrowNodesToThrownNode(body);
	}
}

// End of File
