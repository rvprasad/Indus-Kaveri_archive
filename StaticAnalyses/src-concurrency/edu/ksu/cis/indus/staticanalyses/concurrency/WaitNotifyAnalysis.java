
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

package edu.ksu.cis.indus.staticanalyses.concurrency;

import edu.ksu.cis.indus.common.CollectionsUtilities;
import edu.ksu.cis.indus.common.datastructures.Pair.PairManager;

import edu.ksu.cis.indus.interfaces.ICallGraphInfo;

import edu.ksu.cis.indus.processing.AbstractProcessor;
import edu.ksu.cis.indus.processing.Context;
import edu.ksu.cis.indus.processing.ProcessingController;

import edu.ksu.cis.indus.staticanalyses.concurrency.escape.EquivalenceClassBasedEscapeAnalysis;
import edu.ksu.cis.indus.staticanalyses.interfaces.AbstractAnalysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import soot.SootMethod;

import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;


/**
 * DOCUMENT ME!
 * <p></p>
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public final class WaitNotifyAnalysis
  extends AbstractAnalysis {
	/** 
	 * <p>DOCUMENT ME! </p>
	 */
	final Collection uncoupledWaits = new HashSet();

	/** 
	 * <p>DOCUMENT ME! </p>
	 */
	ICallGraphInfo callgraph;

	/** 
	 * <p>DOCUMENT ME! </p>
	 */
	final Map method2notifies = new HashMap();

	/** 
	 * <p>DOCUMENT ME! </p>
	 */
	final Map method2waits = new HashMap();

	/** 
	 * <p>DOCUMENT ME! </p>
	 */
	final PairManager pairMgr = new PairManager();

	/** 
	 * <p>DOCUMENT ME! </p>
	 */
	EquivalenceClassBasedEscapeAnalysis ecba;

	/**
	 * DOCUMENT ME!
	 * <p></p>
	 *
	 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
	 * @author $Author$
	 * @version $Revision$ $Date$
	 */
	private final class PreProcessor
	  extends AbstractProcessor {
		/**
		 * @see edu.ksu.cis.indus.processing.IProcessor#callback(soot.jimple.Stmt, edu.ksu.cis.indus.processing.Context)
		 */
		public void callback(final Stmt stmt, final Context context) {
			final SootMethod _currentMethod = context.getCurrentMethod();

			if (SafeLockAnalysis.isWaitInvocation((InvokeStmt) stmt, _currentMethod, callgraph)) {
				CollectionsUtilities.putIntoSetInMap(method2waits, _currentMethod, stmt);
			} else if (SafeLockAnalysis.isNotifyInvocation((InvokeStmt) stmt, _currentMethod, callgraph)) {
				CollectionsUtilities.putIntoSetInMap(method2notifies, _currentMethod, stmt);
			}
		}

		/**
		 * @see edu.ksu.cis.indus.processing.IProcessor#consolidate()
		 */
		public void consolidate() {
			for (final Iterator _i = method2waits.keySet().iterator(); _i.hasNext();) {
				final SootMethod _wMethod = (SootMethod) _i.next();
				final InvokeStmt _wStmt = (InvokeStmt) method2waits.get(_wMethod);
				boolean _uncoupled = true;

				for (final Iterator _j = method2notifies.keySet().iterator(); _j.hasNext() && _uncoupled;) {
					final SootMethod _nMethod = (SootMethod) _j.next();
					final InvokeStmt _nStmt = (InvokeStmt) method2notifies.get(_nMethod);
					_uncoupled = !ecba.areWaitAndNotifyCoupled(_wStmt, _wMethod, _nStmt, _nMethod);
				}

				if (_uncoupled) {
					uncoupledWaits.add(pairMgr.getOptimizedPair(_wStmt, _wMethod));
				}
			}
		}

		/**
		 * @see edu.ksu.cis.indus.processing.IProcessor#hookup(edu.ksu.cis.indus.processing.ProcessingController)
		 */
		public void hookup(ProcessingController ppc) {
			ppc.register(InvokeStmt.class, this);
		}

		/**
		 * @see edu.ksu.cis.indus.processing.IProcessor#unhook(edu.ksu.cis.indus.processing.ProcessingController)
		 */
		public void unhook(ProcessingController ppc) {
			ppc.unregister(InvokeStmt.class, this);
		}
	}

	/**
	 * DOCUMENT ME! <p></p>
	 *
	 * @param waitStmt DOCUMENT ME!
	 * @param method DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	public boolean isNotifyValid(final InvokeStmt waitStmt, final SootMethod method) {
		return false;
	}

	/**
	 * DOCUMENT ME! <p></p>
	 *
	 * @param stmt DOCUMENT ME!
	 * @param method DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	public boolean isWaitCoupled(final InvokeStmt stmt, final SootMethod method) {
		return uncoupledWaits.contains(pairMgr.getOptimizedPair(stmt, method));
	}

	/**
	 * DOCUMENT ME! <p></p>
	 *
	 * @param stmt DOCUMENT ME!
	 * @param method DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	public boolean isWaitValid(final InvokeStmt stmt, final SootMethod method) {
		return false;
	}

	/**
	 * DOCUMENT ME! <p></p>
	 */
	public void analyze() {
	}
}

/*
   ChangeLog:
   $Log$
   Revision 1.1  2004/07/23 13:09:44  venku
   - Refactoring in progress.
     - Extended IMonitorInfo interface.
     - Teased apart the logic to calculate monitor info from SynchronizationDA
       into MonitorAnalysis.
     - Casted EquivalenceClassBasedEscapeAnalysis as an AbstractAnalysis.
     - ripple effect.
     - Implemented safelock analysis to handle intraprocedural processing.

 */
