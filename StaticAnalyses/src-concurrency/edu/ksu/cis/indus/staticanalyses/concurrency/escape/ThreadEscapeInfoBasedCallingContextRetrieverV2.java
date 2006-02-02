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

package edu.ksu.cis.indus.staticanalyses.concurrency.escape;

import edu.ksu.cis.indus.common.collections.CollectionUtils;
import edu.ksu.cis.indus.common.collections.Stack;
import edu.ksu.cis.indus.common.soot.Util;
import edu.ksu.cis.indus.interfaces.IEscapeInfo;
import edu.ksu.cis.indus.interfaces.ICallGraphInfo.CallTriple;
import edu.ksu.cis.indus.processing.Context;
import edu.ksu.cis.indus.staticanalyses.dependency.IDependencyAnalysis;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.MonitorStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;

/**
 * This implementation facilitates the extraction of calling-contexts based on multithread data sharing (more precise than
 * escape information).
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class ThreadEscapeInfoBasedCallingContextRetrieverV2
		extends ThreadEscapeInfoBasedCallingContextRetriever {

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadEscapeInfoBasedCallingContextRetrieverV2.class);

	/**
	 * This indicates if this instance retrieves context required to preserve interference dependence.
	 */
	protected final boolean interferenceBased;

	/**
	 * This indicates if this instance retrieves context required to preserve ready dependence.
	 */
	protected final boolean readyBased;

	/**
	 * Creates an instance of this instance.
	 * 
	 * @param callContextLenLimit <i>refer to the constructor of the super class</i>.
	 * @param dependenceID id of the dependence type for which this retriever is used.
	 * @pre depenenceID != null
	 * @pre dependenceId.equals(IDependencyAnalysis.DependenceSort.READY_DA) or
	 *      dependenceId.equals(IDependencyAnalysis.DependenceSort.INTERFERENCE_DA)
	 */
	public ThreadEscapeInfoBasedCallingContextRetrieverV2(final int callContextLenLimit, final Object dependenceID) {
		super(callContextLenLimit);
		readyBased = dependenceID.equals(IDependencyAnalysis.DependenceSort.READY_DA);
		interferenceBased = dependenceID.equals(IDependencyAnalysis.DependenceSort.INTERFERENCE_DA);
	}

	/**
	 * @see ThreadEscapeInfoBasedCallingContextRetriever#considerProgramPoint(Context)
	 */
	@Override protected boolean considerProgramPoint(final Context context) {
		boolean _result = super.considerProgramPoint(context);

		if (_result) {
			final Value _value = context.getProgramPoint().getValue();
			final Stmt _stmt = context.getStmt();
			final SootMethod _currentMethod = context.getCurrentMethod();

			if (interferenceBased) {
				if (_stmt.containsFieldRef()) {
					final FieldRef _fr = _stmt.getFieldRef();

					if (_fr instanceof InstanceFieldRef && ((InstanceFieldRef) _fr).getBase() == _value) {
						_result = escapesInfo.fieldAccessShared(_value, _currentMethod, _fr.getField().getSignature(),
								IEscapeInfo.READ_WRITE_SHARED_ACCESS);
					} else if (_fr instanceof StaticFieldRef && _fr == _value) {
						final SootField _field = _fr.getField();
						final SootClass _declaringClass = _field.getDeclaringClass();
						final String _signature = _field.getSignature();
						_result = escapesInfo.staticfieldAccessShared(_declaringClass, _currentMethod, _signature,
								IEscapeInfo.READ_WRITE_SHARED_ACCESS);
					}
				} else if (_stmt.containsArrayRef() && _stmt.getArrayRef().getBase() == _value) {
					_result = escapesInfo.fieldAccessShared(_value, _currentMethod, IEscapeInfo.READ_WRITE_SHARED_ACCESS);
				}
			} else if (readyBased) {
				if (_stmt instanceof MonitorStmt && ((MonitorStmt) _stmt).getOp() == _value) {
					_result = escapesInfo.lockUnlockShared(_value, _currentMethod);
				} else if (_stmt.containsInvokeExpr()) {
					final InvokeExpr _ex = _stmt.getInvokeExpr();
					final SootMethod _invokedMethod = _ex.getMethod();

					if (_ex instanceof VirtualInvokeExpr
							&& (Util.isWaitMethod(_invokedMethod) || Util.isNotifyMethod(_invokedMethod))
							&& ((VirtualInvokeExpr) _ex).getBase() == _value) {
						_result = escapesInfo.waitNotifyShared(_value, _currentMethod);
					}
				}
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("considerProgramPoint() - result =" + _result);
		}

		return _result;
	}

	/**
	 * @see ThreadEscapeInfoBasedCallingContextRetriever#considerThis(Context)
	 */
	@Override protected boolean considerThis(final Context methodContext) {
		final SootMethod _method = methodContext.getCurrentMethod();
		final boolean _result1 = interferenceBased
				&& escapesInfo.thisFieldAccessShared(_method, IEscapeInfo.READ_WRITE_SHARED_ACCESS);
		final boolean _result2 = readyBased
				&& (escapesInfo.thisWaitNotifyShared(_method) || escapesInfo.thisLockUnlockShared(_method));

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("considerThis() -  : _result = " + (_result1 || _result2));
		}

		return _result1 || _result2;
	}

	/**
	 * @see ThreadEscapeInfoBasedCallingContextRetriever#getCallerSideToken(java.lang.Object, soot.SootMethod,
	 *      edu.ksu.cis.indus.interfaces.ICallGraphInfo.CallTriple, Stack)
	 */
	@Override protected Object getCallerSideToken(final Object token, final SootMethod callee, final CallTriple callsite,
			final Stack<CallTriple> calleeCallStack) {

		Object _result = super.getCallerSideToken(token, callee, callsite, calleeCallStack);

		if (!(_result instanceof Tokens)) {
			final AliasSet _callerSideToken = (AliasSet) _result;
			final AliasSet _calleeSideToken = (AliasSet) token;

			if (_callerSideToken != null && _calleeSideToken != null
					&& shouldCallerSideTokenBeDiscarded(_callerSideToken, _calleeSideToken)) {
				if (Util.isStartMethod(callee)) {
					_result = Tokens.ACCEPT_NON_TERMINAL_CONTEXT_TOKEN;
				} else {
					_result = Tokens.DISCARD_CONTEXT_TOKEN;
				}
			}
		}

		return _result;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param callerSideToken DOCUMENT ME!
	 * @param calleeSideToken DOCUMENT ME!
	 * @return DOCUMENT ME!
	 */
	protected boolean shouldCallerSideTokenBeDiscarded(final AliasSet callerSideToken, final AliasSet calleeSideToken) {
		final boolean _discardToken;
		if (interferenceBased) {
			final Collection<?> _callerRWEntities = callerSideToken.getReadWriteShareEntities();
			final Collection<?> _calleeRWEntities = calleeSideToken.getReadWriteShareEntities();
			_discardToken = _callerRWEntities == null || _calleeRWEntities == null
					|| CollectionUtils.containsAny(_callerRWEntities, _calleeRWEntities);
		} else if (readyBased) {
			final Collection<?> _callerReadyEntities = callerSideToken.getReadyEntities();
			final Collection<?> _calleeReadyEntities = calleeSideToken.getReadyEntities();
			final boolean _b2 = _callerReadyEntities == null || _calleeReadyEntities == null
					|| CollectionUtils.containsAny(_callerReadyEntities, _calleeReadyEntities);
			final Collection<?> _callerLockEntities = callerSideToken.getLockEntities();
			final Collection<?> _calleeLockEntities = calleeSideToken.getLockEntities();
			_discardToken = _b2
					|| (_callerLockEntities == null || _calleeLockEntities == null || CollectionUtils.containsAny(
							_callerLockEntities, _calleeLockEntities));

		} else {
			_discardToken = false;
		}
		return _discardToken;
	}

}

// End of File
