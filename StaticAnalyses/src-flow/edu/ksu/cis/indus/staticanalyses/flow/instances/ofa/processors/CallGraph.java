
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

package edu.ksu.cis.indus.staticanalyses.flow.instances.ofa.processors;

import edu.ksu.cis.indus.processing.Context;
import edu.ksu.cis.indus.processing.ProcessingController;

import edu.ksu.cis.indus.staticanalyses.flow.instances.ofa.OFAnalyzer;
import edu.ksu.cis.indus.staticanalyses.interfaces.ICallGraphInfo;
import edu.ksu.cis.indus.staticanalyses.interfaces.IValueAnalyzer;
import edu.ksu.cis.indus.staticanalyses.processing.AbstractValueAnalyzerBasedProcessor;
import edu.ksu.cis.indus.staticanalyses.support.DirectedGraph;
import edu.ksu.cis.indus.staticanalyses.support.FIFOWorkBag;
import edu.ksu.cis.indus.staticanalyses.support.IWorkBag;
import edu.ksu.cis.indus.staticanalyses.support.MutableDirectedGraph.MutableNode;
import edu.ksu.cis.indus.staticanalyses.support.SimpleNodeGraph;
import edu.ksu.cis.indus.staticanalyses.support.SimpleNodeGraph.SimpleNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.ValueBox;

import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;


/**
 * This class calculates call graphCache information from the given object flow analysis.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$
 */
public class CallGraph
  extends AbstractValueAnalyzerBasedProcessor
  implements ICallGraphInfo {
	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Log LOGGER = LogFactory.getLog(CallGraph.class);

	/**
	 * This indicates if the processor has stabilized.  If so, it is safe to query this object for information. By default,
	 * this field is initialized to indicate that the processor is in a stable state.  The subclasses will need to toggle it
	 * suitably.
	 */
	protected boolean stable = true;

	/**
	 * The collection of methods from which the system can be started.  Although an instance of a class can be created and a
	 * method can be invoked on it from the environment, this method will not be considered as a <i>head method</i>.
	 * However, our definition of head methods are those methods(excluding those in invoked via <code>invokespecial</code>
	 * bytecode) with no caller method that belongs to the system.
	 *
	 * @invariant head != null and heads.oclIsKindOf(Set(SootMethod))
	 */
	private final Collection heads = new HashSet();

	/**
	 * The collection of SCCs in this call graph in top-down direction.
	 *
	 * @invariant bottomUpSCC.oclIsKindOf(Set(Collection(SootMethod)))
	 */
	private Collection bottomUpSCC;

	/**
	 * The collection of methods that are reachble in the system.
	 *
	 * @invariant reachables.oclIsKindOf(Set(SootMethod))
	 */
	private Collection reachables = new HashSet();

	/**
	 * The collection of SCCs in this call graph in top-down direction.
	 *
	 * @invariant topDownSCC.oclIsKindOf(Set(Collection(SootMethod)))
	 */
	private Collection topDownSCC;

	/**
	 * The FA instance which implements object flow analysis.  This instance is used to calculate call graphCache
	 * information.
	 *
	 * @invariant analyzer.oclIsKindOf(OFAnalyzer)
	 */
	private IValueAnalyzer analyzer;

	/**
	 * This maps callees to callers.
	 *
	 * @invariant callee2callers.oclIsKindOf(Map(SootMethod, Set(CallTriple)))
	 */
	private Map callee2callers = new HashMap();

	/**
	 * This maps callers to callees.
	 *
	 * @invariant caller2callees.oclIsKindOf(Map(SootMethod, Set(CallTriple)))
	 */
	private Map caller2callees = new HashMap();	

	/**
	 * This caches a traversable graphCache representation of the call graphCache.
	 */
	private SimpleNodeGraph graphCache;

	/**
	 * Sets the analyzer to be used to calculate call graph information upon call back.
	 *
	 * @param objFlowAnalyzer that provides the information to create the call graph.
	 *
	 * @pre objFlowAnalyzer != null and objFlowAnalyzer.oclIsKindOf(OFAnalyzer)
	 *
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.IValueAnalyzerBasedProcessor#setAnalyzer(IValueAnalyzer)
	 */
	public void setAnalyzer(final IValueAnalyzer objFlowAnalyzer) {
		this.analyzer = (OFAnalyzer) objFlowAnalyzer;
		heads.clear();
		reachables.clear();
		graphCache = null;
	}

	/**
	 * Returns a collection of methods called by <code>caller</code>.
	 *
	 * @param caller which calls the returned methods.
	 *
	 * @return a collection of call sites along with callees at those sites.
	 *
	 * @pre caller != null
	 * @post result.oclIsKindOf(Collection(CallTriple))
	 *
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.ICallGraphInfo#getCallees(SootMethod)
	 */
	public Collection getCallees(final SootMethod caller) {
		Collection result = Collections.EMPTY_LIST;
		Collection callees = (Collection) caller2callees.get(caller);

		if (callees != null) {
			result = Collections.unmodifiableCollection(callees);
		}
		return result;
	}

	/**
	 * Returns the set of method implementations that shall be invoked at the given callsite expression in the given method.
	 *
	 * @param invokeExpr the method call site.
	 * @param context in which the call occurs.
	 *
	 * @return a collection of methods.
	 *
	 * @pre invokeExpr != null and context != null
	 * @pre context.getCurrentMethod() != null
	 * @pre contet.getStmt() != null
	 * @post result.oclIsKindOf(Collection(SootMethod))
	 *
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.ICallGraphInfo#getCallees(InvokeExpr,Context)
	 */
	public Collection getCallees(final InvokeExpr invokeExpr, final Context context) {
		Collection result;

		Collection temp = (Collection) caller2callees.get(context.getCurrentMethod());

		if (temp != null && !temp.isEmpty()) {
			result = new ArrayList();

			for (Iterator i = temp.iterator(); i.hasNext();) {
				CallTriple ctrp = (CallTriple) i.next();

				if (ctrp.getExpr().equals(invokeExpr)) {
					result.add(ctrp.getMethod());
				}
			}
		} else {
			result = Collections.EMPTY_LIST;
		}

		return result;
	}

	/**
	 * Returns the methods that call the given method independent of any context.
	 *
	 * @param callee is the method being called.
	 *
	 * @return a collection of call-sites at which <code>callee</code> is called.
	 *
	 * @pre callee != null
	 * @post result->forall(o | o.oclIsKindOf(CallTriple))
	 *
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.ICallGraphInfo#getCallers(soot.SootMethod)
	 */
	public Collection getCallers(final SootMethod callee) {
		Collection result = Collections.EMPTY_LIST;
		Collection callers = (Collection) callee2callers.get(callee);

		if (callers != null) {
			result = Collections.unmodifiableCollection(callers);
		}
		return result;
	}

	/**
	 * Returns the methods that are the entry point for the analyzed system.
	 *
	 * @return a collection of methods.
	 *
	 * @post result->forall(o | o.oclType = SootMethod)
	 *
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.ICallGraphInfo#getHeads()
	 */
	public Collection getHeads() {
		return Collections.unmodifiableCollection(heads);
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.ICallGraphInfo#getMethodsReachableFrom(soot.jimple.Stmt,
	 * 		soot.SootMethod)
	 */
	public Collection getMethodsReachableFrom(final Stmt stmt, final SootMethod root) {
		Collection result = new HashSet();
		InvokeExpr ie = stmt.getInvokeExpr();
		Context context = new Context();
		context.setRootMethod(root);
		result.add(getCallees(ie, context));

		IWorkBag wb = new FIFOWorkBag();
		wb.addAllWorkNoDuplicates(result);

		while (wb.hasWork()) {
			SootMethod callee = (SootMethod) wb.getWork();

			if (!result.contains(callee)) {
				Collection callees = CollectionUtils.subtract(getCallees(callee), result);
				wb.addAllWorkNoDuplicates(callees);
				result.addAll(callees);
			}
		}
		return result;
	}

	/**
	 * Checks if the given method is reachable in the analyzed system.
	 *
	 * @param method to be checked for reachabiliy.
	 *
	 * @return <code>true</code> if <code>method</code> is reachable; <code>false</code>, otherwise.
	 *
	 * @pre method != null
	 *
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.ICallGraphInfo#isReachable(soot.SootMethod)
	 */
	public boolean isReachable(final SootMethod method) {
		return reachables.contains(method);
	}

	/**
	 * Returns the methods reachable in the analyzed system.
	 *
	 * @return a collection of methods.
	 *
	 * @post result->forall(o | o.oclType = SootMethod)
	 *
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.ICallGraphInfo#getReachableMethods()
	 */
	public Collection getReachableMethods() {
		return Collections.unmodifiableCollection(reachables);
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.ICallGraphInfo#getSCCs(boolean)
	 */
	public Collection getSCCs(final boolean topDown) {
		Collection result = new HashSet();
		Collection temp;

		if (topDown) {
			if (topDownSCC == null) {
				topDownSCC = new HashSet();
				temp = graphCache.getSCCs(true);

				for (Iterator i = temp.iterator(); i.hasNext();) {
					Collection scc = (Collection) i.next();
					java.util.List l = new ArrayList();

					for (Iterator j = scc.iterator(); j.hasNext();) {
						l.add(((SimpleNode) j.next())._object);
					}
					topDownSCC.add(l);
				}
			}
			temp = topDownSCC;
		} else {
			if (bottomUpSCC == null) {
				bottomUpSCC = new HashSet();
				temp = graphCache.getSCCs(false);

				for (Iterator i = temp.iterator(); i.hasNext();) {
					Collection scc = (Collection) i.next();
					java.util.List l = new ArrayList();

					for (Iterator j = scc.iterator(); j.hasNext();) {
						l.add(((SimpleNode) j.next())._object);
					}
					bottomUpSCC.add(l);
				}
			}
			temp = bottomUpSCC;
		}

		for (Iterator i = temp.iterator(); i.hasNext();) {
			java.util.List scc = (java.util.List) i.next();
			result.add(Collections.unmodifiableList(scc));
		}
		return result;
	}

	/**
	 * @see edu.ksu.cis.indus.interfaces.IStatus#isStable()
	 */
	public boolean isStable() {
		return stable;
	}

	/**
	 * Called by the post process controller when it walks a jimple value AST node.
	 *
	 * @param vBox is the AST node to be processed.
	 * @param context in which value should be processed.
	 *
	 * @pre context != null
	 *
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.IValueAnalyzerBasedProcessor#callback(Value,Context)
	 */
	public void callback(final ValueBox vBox, final Context context) {
		Stmt stmt = context.getStmt();
		SootMethod caller = context.getCurrentMethod();
		SootMethod callee = null;
		Set callees;
		Set callers;
		CallTriple triple;
		Value value = vBox.getValue();

		if (value instanceof StaticInvokeExpr) {
			InvokeExpr invokeExpr = (InvokeExpr) value;
			callee = invokeExpr.getMethod();

			if (caller2callees.containsKey(caller)) {
				callees = (Set) caller2callees.get(caller);
			} else {
				callees = new HashSet();
				caller2callees.put(caller, callees);
			}
			triple = new CallTriple(callee, stmt, invokeExpr);
			callees.add(triple);

			if (callee2callers.containsKey(callee)) {
				callers = (Set) callee2callers.get(callee);
			} else {
				callers = new HashSet();
				callee2callers.put(callee, callers);
			}
			triple = new CallTriple(caller, stmt, invokeExpr);
			callers.add(triple);
		} else if (value instanceof InterfaceInvokeExpr
			  || value instanceof VirtualInvokeExpr
			  || value instanceof SpecialInvokeExpr) {
			InstanceInvokeExpr invokeExpr = (InstanceInvokeExpr) value;
			SootMethod calleeMethod = invokeExpr.getMethod();
			context.setProgramPoint(invokeExpr.getBaseBox());

			Collection values = analyzer.getValues(invokeExpr.getBase(), context);

			if (!values.isEmpty()) {
				if (caller2callees.containsKey(caller)) {
					callees = (Set) caller2callees.get(caller);
				} else {
					callees = new HashSet();
					caller2callees.put(caller, callees);
				}

				CallTriple ctrp = new CallTriple(caller, stmt, invokeExpr);

				for (Iterator i = values.iterator(); i.hasNext();) {
					Object t = i.next();

					if (!(t instanceof NewExpr)) {
						continue;
					}

					NewExpr newExpr = (NewExpr) t;
					SootClass accessClass = analyzer.getEnvironment().getClass(newExpr.getBaseType().getClassName());
					callee = findMethodImplementation(accessClass, calleeMethod);

					triple = new CallTriple(callee, stmt, invokeExpr);
					callees.add(triple);

					if (callee2callers.containsKey(callee)) {
						callers = (Set) callee2callers.get(callee);
					} else {
						callers = new HashSet();
						callee2callers.put(callee, callers);
					}
					callers.add(ctrp);
				}
			}
		}
	}

	/**
	 * @see edu.ksu.cis.indus.processing.IProcessor#callback(soot.SootMethod)
	 */
	public void callback(final SootMethod method) {
		// all method marked by the object flow analyses are reachable.
		reachables.add(method);

		if (method.getName().equals("<clinit>")) {
			heads.add(method);
		}
	}

	/**
	 * This calculates information such as heads, tails, and such.
	 *
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.IValueAnalyzerBasedProcessor#consolidate()
	 */
	public void consolidate() {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("BEGIN: call graph consolidation");
		}

		long start = System.currentTimeMillis();
		heads.addAll(analyzer.getEnvironment().getRoots());

		// populate the caller2callees with head information in cases there are no calls in the system.
		if (caller2callees.isEmpty()) {
			for (Iterator i = heads.iterator(); i.hasNext();) {
				caller2callees.put(i.next(), Collections.EMPTY_LIST);
			}
		}

		// construct call graph 
		graphCache = new SimpleNodeGraph();

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Starting construction of call graph...");
		}

		Collection c = new ArrayList();
		Collection temp;

		for (Iterator i = reachables.iterator(); i.hasNext();) {
			SootMethod sm = (SootMethod) i.next();
			temp = (Collection) caller2callees.get(sm);

			if (temp != null) {
				c.clear();

				MutableNode callerNode = graphCache.getNode(sm);

				for (Iterator j = temp.iterator(); j.hasNext();) {
					CallTriple ctrp = (CallTriple) j.next();
					SootMethod method = ctrp.getMethod();

					graphCache.addEdgeFromTo(callerNode, graphCache.getNode(method));
				}
				temp.removeAll(c);
			}
		}

		long stop = System.currentTimeMillis();

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("END: call graph consolidation");
			LOGGER.info("TIMING: call graph consolidation took " + (stop - start) + "ms.");
		}
	}

	/**
	 * Provides a stringized representation of this call graphCache.
	 *
	 * @return stringized representation of the this call graphCache.
	 */
	public String dumpGraph() {
		StringBuffer result = new StringBuffer();

		result.append("Root of the system: ");

		for (Iterator i = getHeads().iterator(); i.hasNext();) {
			result.append("\t" + ((SootMethod) i.next()).getSignature());
		}
		result.append("\nReachable methods in the system: " + getReachableMethods().size() + "\n");
		result.append("Strongly Connected components in the system: " + getSCCs(true).size() + "\n");
		result.append("top-down\n");

		for (Iterator i = caller2callees.entrySet().iterator(); i.hasNext();) {
			Map.Entry entry = (Map.Entry) i.next();
			SootMethod caller = (SootMethod) entry.getKey();
			result.append("\n" + caller.getSignature() + "\n");

			Collection callees = (Collection) entry.getValue();

			for (Iterator j = callees.iterator(); j.hasNext();) {
				CallTriple ctrp = (CallTriple) j.next();
				result.append("\t" + ctrp.getMethod().getSignature() + "\n");
			}
		}

		result.append("bottom-up\n");

		for (Iterator i = callee2callers.entrySet().iterator(); i.hasNext();) {
			Map.Entry entry = (Map.Entry) i.next();
			SootMethod callee = (SootMethod) entry.getKey();
			result.append("\n" + callee.getSignature() + "\n");

			Collection callers = (Collection) entry.getValue();

			for (Iterator j = callers.iterator(); j.hasNext();) {
				CallTriple ctrp = (CallTriple) j.next();
				result.append("\t" + ctrp.getMethod().getSignature() + "\n");
			}
		}

		return result.toString();
	}

	/**
	 * @see edu.ksu.cis.indus.interfaces.IProcessor#hookup(ProcessingController)
	 */
	public void hookup(final ProcessingController ppc) {
		stable = false;
		ppc.register(VirtualInvokeExpr.class, this);
		ppc.register(InterfaceInvokeExpr.class, this);
		ppc.register(StaticInvokeExpr.class, this);
		ppc.register(SpecialInvokeExpr.class, this);
		ppc.register(this);
	}

	/**
	 * Resets all internal data structure and forgets all info from the previous run.
	 */
	public void reset() {
		caller2callees.clear();
		callee2callers.clear();
		analyzer = null;
		graphCache = null;
		topDownSCC = null;
		bottomUpSCC = null;
		reachables.clear();
		heads.clear();
	}

	/**
	 * @see edu.ksu.cis.indus.interfaces.IProcessor#unhook(ProcessingController)
	 */
	public void unhook(final ProcessingController ppc) {
		ppc.unregister(VirtualInvokeExpr.class, this);
		ppc.unregister(InterfaceInvokeExpr.class, this);
		ppc.unregister(StaticInvokeExpr.class, this);
		ppc.unregister(SpecialInvokeExpr.class, this);
		ppc.unregister(this);
		stable = true;
	}

	/**
	 * Testing purposes only.
	 *
	 * @return the cached copy of the call graph.
	 */
	final DirectedGraph getCallGraph() {
		return graphCache;
	}

	/**
	 * Finds the implementation of <code>method</code> when accessed via <code>accessClass</code>.
	 *
	 * @param accessClass is the class via which <code>method</code> is accesed.
	 * @param method being accessed/invoked.
	 *
	 * @return the implementation of <code>method</code> if present in the class hierarchy; <code>null</code>, otherwise.
	 *
	 * @pre accessClass != null and method != null
	 */
	private SootMethod findMethodImplementation(final SootClass accessClass, final SootMethod method) {
		String methodName = method.getName();
		List parameterTypes = method.getParameterTypes();
		Type returnType = method.getReturnType();
		return findMethodImplementation(accessClass, methodName, parameterTypes, returnType);
	}

	/**
	 * Finds the implementation of the given method when accessed via <code>accessClass</code>.
	 *
	 * @param accessClass is the class via which the method is invoked.
	 * @param methodName is the name of the method.
	 * @param parameterTypes is the list of parameter types of the method.
	 * @param returnType is the return type of the method.
	 *
	 * @return the implementation of the requested method if present in the class hierarchy; <code>null</code>, otherwise.
	 *
	 * @pre accessClass != null and methodName != null and parameterTypes != null and returnType != null
	 */
	private SootMethod findMethodImplementation(final SootClass accessClass, final String methodName,
		final List parameterTypes, final Type returnType) {
		SootMethod result = null;

		if (accessClass.declaresMethod(methodName, parameterTypes, returnType)) {
			result = accessClass.getMethod(methodName, parameterTypes, returnType);
		} else {
			if (accessClass.hasSuperclass()) {
				SootClass superClass = accessClass.getSuperclass();
				result = findMethodImplementation(superClass, methodName, parameterTypes, returnType);
			} else {
				if (LOGGER.isErrorEnabled()) {
					LOGGER.error(methodName + "(" + parameterTypes + "):" + returnType + " is not accessible from "
						+ accessClass);
				}
			}
		}
		return result;
	}
}

/*
   ChangeLog:
   $Log$
   Revision 1.35  2003/12/07 08:41:32  venku
   - deleted getCallGraph() from ICallGraphInfo interface.
   - made getSCCs() direction sensitive.
   - ripple effect.
   Revision 1.34  2003/12/05 21:02:25  venku
   - special invokes are treated just like virtual invoke.
   Revision 1.33  2003/12/02 09:42:38  venku
   - well well well. coding convention and formatting changed
     as a result of embracing checkstyle 3.2
   Revision 1.32  2003/11/29 09:34:59  venku
   - removed getCycles() method as it was not being used.
   Revision 1.31  2003/11/29 09:30:37  venku
   - removed getRecursionRoots() method as it was not being used.
   - modified pruning algorithmm.
   - modified getCallees(InvokeExpr,Context) method.
   Revision 1.30  2003/11/28 22:10:34  venku
   - formatting.
   - simple and faster pruning algorithm.
   Revision 1.29  2003/11/26 06:14:54  venku
   - formatting and coding convention.
   Revision 1.28  2003/11/26 02:55:45  venku
   - now handles clinit in a more robust way.
   Revision 1.27  2003/11/25 23:48:23  venku
   - added support to consider <clinit> methods as well.
   Revision 1.26  2003/11/17 15:42:46  venku
   - changed the signature of callback(Value,..) to callback(ValueBox,..)
   Revision 1.25  2003/11/10 03:17:19  venku
   - renamed AbstractProcessor to AbstractValueAnalyzerBasedProcessor.
   - ripple effect.
   Revision 1.24  2003/11/06 05:31:08  venku
   - moved IProcessor to processing package from interfaces.
   - ripple effect.
   - fixed documentation errors.
   Revision 1.23  2003/11/06 05:15:07  venku
   - Refactoring, Refactoring, Refactoring.
   - Generalized the processing controller to be available
     in Indus as it may be useful outside static anlaysis. This
     meant moving IProcessor, Context, and ProcessingController.
   - ripple effect of the above changes was large.
   Revision 1.22  2003/11/05 09:32:48  venku
   - ripple effect of splitting Workbag.
   Revision 1.21  2003/09/29 06:54:57  venku
   - dump formatting.
   Revision 1.20  2003/09/29 06:19:34  venku
   - added more info to the dump.
   Revision 1.19  2003/09/29 05:52:44  venku
   - added more info to the dump.
   Revision 1.18  2003/09/28 03:16:33  venku
   - I don't know.  cvs indicates that there are no differences,
     but yet says it is out of sync.
   Revision 1.17  2003/09/25 03:30:19  venku
   - coding convention.
   Revision 1.16  2003/09/13 04:24:42  venku
   - boundary conditions, boundary conditions.  Well, we did not
     handle the case when there was no calls in the system. FIXED.
   Revision 1.15  2003/09/12 01:24:12  venku
   - As preprocessing before CallGraph exists happen based on the
     parts of the system touched.  Non-reachable methods will be
     pre-processed.  However, later they should be pruned away. FIXED.
   Revision 1.14  2003/09/08 02:07:44  venku
   - debug stmt error. FIXED.
   Revision 1.13  2003/08/25 09:31:39  venku
   Enabled reset() support for these classes.
   Revision 1.12  2003/08/24 08:13:11  venku
   Major refactoring.
    - The methods to modify the graphs were exposed.
    - The above anamoly was fixed by supporting a new class MutableDirectedGraph.
    - Each Mutable graph extends this graph and exposes itself via
      suitable interface to restrict access.
    - Ripple effect of the above changes.
   Revision 1.11  2003/08/21 03:43:56  venku
   Ripple effect of adding IStatus.
   Revision 1.10  2003/08/17 11:54:25  venku
   Formatting and documentation.
   Revision 1.9  2003/08/17 10:48:34  venku
   Renamed BFA to FA.  Also renamed bfa variables to fa.
   Ripple effect was huge.
   Revision 1.8  2003/08/15 23:23:32  venku
   Removed redundant "implement IValueAnalyzerBasedProcessor".
   Revision 1.7  2003/08/14 05:10:29  venku
   Fixed documentation links.
   Revision 1.6  2003/08/13 08:49:10  venku
   Spruced up documentation and specification.
   Tightened preconditions in the interface such that they can be loosed later on in implementaions.
   Revision 1.5  2003/08/13 08:29:40  venku
   Spruced up documentation and specification.
   Revision 1.4  2003/08/12 18:20:43  venku
   Ripple effect of changing the analyzer and the environment.
   Revision 1.3  2003/08/11 04:27:34  venku
   - Ripple effect of changes to Pair
   - Ripple effect of changes to _content in Marker
   - Changes of how thread start sites are tracked in ThreadGraphInfo
   Revision 1.2  2003/08/09 21:54:00  venku
   Leveraging getInvokeExpr() in Stmt class in getMethodsReachableFrom()
   Revision 1.1  2003/08/07 06:40:24  venku
   Major:
    - Moved the package under indus umbrella.
 */
