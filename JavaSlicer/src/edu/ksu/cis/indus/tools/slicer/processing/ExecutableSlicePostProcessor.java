
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

package edu.ksu.cis.indus.tools.slicer.processing;

import edu.ksu.cis.indus.common.datastructures.FIFOWorkBag;
import edu.ksu.cis.indus.common.datastructures.IWorkBag;
import edu.ksu.cis.indus.common.graph.BasicBlockGraph;
import edu.ksu.cis.indus.common.graph.BasicBlockGraph.BasicBlock;
import edu.ksu.cis.indus.common.graph.BasicBlockGraphMgr;

import edu.ksu.cis.indus.slicer.SliceCollector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Trap;
import soot.TrapManager;
import soot.Value;
import soot.ValueBox;

import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.ThisRef;


/**
 * DOCUMENT ME!
 * 
 * <p></p>
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public final class ExecutableSlicePostProcessor
  implements ISlicePostProcessor {
	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Log LOGGER = LogFactory.getLog(ExecutableSlicePostProcessor.class);

	/**
	 * <p>
	 * DOCUMENT ME!
	 * </p>
	 */
	private BasicBlockGraphMgr bbgMgr;

	/**
	 * <p>
	 * DOCUMENT ME!
	 * </p>
	 */
	private final Collection processed = new HashSet();

	/**
	 * <p>
	 * DOCUMENT ME!
	 * </p>
	 */
	private final IWorkBag workBag = new FIFOWorkBag();

	/**
	 * <p>
	 * DOCUMENT ME!
	 * </p>
	 */
	private SliceCollector collector;

	/**
	 * DOCUMENT ME!
	 * 
	 * <p></p>
	 *
	 * @param taggedMethods DOCUMENT ME!
	 * @param basicBlockMgr DOCUMENT ME!
	 * @param theCollector DOCUMENT ME!
	 */
	public void process(final Collection taggedMethods, final BasicBlockGraphMgr basicBlockMgr,
		final SliceCollector theCollector) {
		collector = theCollector;
		bbgMgr = basicBlockMgr;
		workBag.addAllWorkNoDuplicates(taggedMethods);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("BEGIN: Post Processing.");
		}

		while (workBag.hasWork()) {
			final SootMethod _method = (SootMethod) workBag.getWork();
			processed.add(_method);

			if (_method.isConcrete()) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Post Processing method " + _method);
				}
				processStmts(_method);
				pickReturnPoints(_method);
				pruneLocals(_method);
			} else {
				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn("Could not get body for method " + _method.getSignature());
				}
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("END: Post Processing.");
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * <p></p>
	 */
	public void reset() {
		workBag.clear();
		processed.clear();
		bbgMgr = null;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * <p></p>
	 *
	 * @param _sm DOCUMENT ME!
	 */
	private void addToWorkBag(SootMethod _sm) {
		if (!processed.contains(_sm)) {
			workBag.addWorkNoDuplicates(_sm);
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * <p></p>
	 *
	 * @param method DOCUMENT ME!
	 */
	private void pickReturnPoints(final SootMethod method) {
		// pick all return/throw points in the methods.
		final BasicBlockGraph _bbg = bbgMgr.getBasicBlockGraph(method);
		final Collection _tails = _bbg.getTails();

		for (final Iterator _j = _tails.iterator(); _j.hasNext();) {
			final BasicBlock _bb = (BasicBlock) _j.next();
			final Stmt _stmt = _bb.getTrailerStmt();

			if (_stmt.getTag(collector.getTagName()) == null) {
				collector.includeInSlice(_stmt);
				collector.includeInSlice(method);

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Picked " + _stmt + " in " + method);
				}
			}
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * <p></p>
	 *
	 * @param method DOCUMENT ME!
	 */
	private void processStmts(final SootMethod method) {
		final Body _body = method.retrieveActiveBody();
		final List _sl = new ArrayList(_body.getUnits());
		final Collection _trapsToKeep = new ArrayList();

		for (final Iterator _j = _sl.iterator(); _j.hasNext();) {
			final Stmt _stmt = (Stmt) _j.next();

			if (_stmt instanceof IdentityStmt) {
				/*
				 * Pick all identity statements in the program that retrieve parameters and this reference upon
				 * entrance into the method. Note that it is required that all elements pushed to the stack should be
				 * popped before returning with only the return value on the stack (if it exists).  This does not
				 * mean that all parameters need to be popped off the stack upon entry.  Hence, we should scan the
				 * entire body for Identity statements with ParameterRef or ThisRef on RHS.
				 */
				final IdentityStmt _id = (IdentityStmt) _stmt;
				final Value _rhs = _id.getRightOp();

				if (_rhs instanceof ThisRef || _rhs instanceof ParameterRef) {
					collector.includeInSlice(_id.getLeftOpBox());
					collector.includeInSlice(_id.getRightOpBox());
					collector.includeInSlice(_id);

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Picked " + _stmt + " in " + method);
					}
				}
			} else if (_stmt.containsInvokeExpr()) {
				/*
				 * If an invoke expression occurs, the slice will include only the invoked method and not any
				 * incarnations of it in it's ancestral classes.  This will lead to unverifiable system of classes.
				 * This can be fixed by sucking all the method definitions that need to make the system verifiable
				 * and empty bodies will be substituted for such methods.
				 */
				final InvokeExpr _expr = _stmt.getInvokeExpr();

				if (!(_expr instanceof StaticInvokeExpr)) {
					final SootMethod _sm = _expr.getMethod();
					collector.includeInSlice(_sm);

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Included method invoked at " + _stmt + " in " + method);
					}
					addToWorkBag(_sm);
				}
			}

			// calculate the relevant traps
			if (collector.hasBeenCollected(_stmt)) {
				_trapsToKeep.addAll(TrapManager.getTrapsAt(_stmt, _body));
			}
		}

		// delete unwanted traps
		final Collection _traps = _body.getTraps();
		_traps.retainAll(_trapsToKeep);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Deleting traps " + CollectionUtils.subtract(_traps, _trapsToKeep) + " from " + method);
		}

		/*
		 * Include the first statement of the handler for all traps found to cover atleast one statement included in the
		 * slice.
		 */
		for (final Iterator _i = _trapsToKeep.iterator(); _i.hasNext();) {
			final Trap _trap = (Trap) _i.next();
			final IdentityStmt _handlerUnit = (IdentityStmt) _trap.getHandlerUnit();
			collector.includeInSlice(_handlerUnit);
			collector.includeInSlice(_handlerUnit.getLeftOpBox());
			collector.includeInSlice(_handlerUnit.getRightOpBox());
			collector.includeInSlice(_trap.getException());
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * <p></p>
	 *
	 * @param method DOCUMENT ME!
	 */
	private void pruneLocals(final SootMethod method) {
		final Body _body = method.getActiveBody();
		final Collection _localsToKeep = new ArrayList();

		for (final Iterator _j = _body.getUnits().iterator(); _j.hasNext();) {
			final Stmt _stmt = (Stmt) _j.next();

			if (collector.hasBeenCollected(_stmt)) {
				for (final Iterator _k = _stmt.getUseAndDefBoxes().iterator(); _k.hasNext();) {
					final ValueBox _vBox = (ValueBox) _k.next();
					final Value _value = _vBox.getValue();

					if (collector.hasBeenCollected(_vBox) && _value instanceof Local) {
						_localsToKeep.add(_value);
					}
				}
			}
		}
		_body.getLocals().retainAll(_localsToKeep);
	}
}

/*
   ChangeLog:
   $Log$
   Revision 1.2  2004/01/14 11:55:45  venku
   - when pruning handlers, we need to include the rhs and lhs of
     the identity statement that occurs at the handler unit.
   Revision 1.1  2004/01/13 10:15:24  venku
   - In terms of post processing we need to do so only when we
     require executable slice and the processing is the same
     independent of the direction of the slice.  Hence, we have just
     one processor instead of 3.  Now we can have specializing
     post processor if we wanted to but I need more application
     information before I decide on this.
   Revision 1.1  2004/01/13 07:53:51  venku
   - as post processing beyond retention of semantics of slice is
     particular to the application or the tool.  Hence, moved the
     post processors into processing package under slicer tool.
   - added a new method to AbstractSliceGotoProcessor to
     process a collection of methods given a basic block graph
     manager.
   Revision 1.2  2004/01/13 04:39:29  venku
   - method and class visibility.
   Revision 1.1  2004/01/13 04:35:08  venku
   - added a new package called "processing" and it will house
     all processing done on the slice to ensure the slice satisfies
     certain properties such as executability.
   - Moved GotoProcessors into processing package.
 */
