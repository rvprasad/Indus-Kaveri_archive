
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

import edu.ksu.cis.indus.common.ToStringBasedComparator;
import edu.ksu.cis.indus.common.datastructures.FIFOWorkBag;
import edu.ksu.cis.indus.common.datastructures.IWorkBag;
import edu.ksu.cis.indus.common.graph.IDirectedGraph;
import edu.ksu.cis.indus.common.graph.INode;
import edu.ksu.cis.indus.common.graph.SimpleNodeGraph;
import edu.ksu.cis.indus.common.graph.SimpleNodeGraph.SimpleNode;

import edu.ksu.cis.indus.interfaces.ICallGraphInfo;

import edu.ksu.cis.indus.processing.Context;
import edu.ksu.cis.indus.processing.ProcessingController;

import edu.ksu.cis.indus.staticanalyses.flow.instances.ofa.OFAnalyzer;
import edu.ksu.cis.indus.staticanalyses.interfaces.IValueAnalyzer;
import edu.ksu.cis.indus.staticanalyses.processing.AbstractValueAnalyzerBasedProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import soot.jimple.StringConstant;
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
	 * The collection of methods that are reachble in the system.
	 *
	 * @invariant reachables.oclIsKindOf(Set(SootMethod))
	 */
	private Collection reachables = new HashSet();

	/**
	 * The FA instance which implements object flow analysis.  This instance is used to calculate call graphCache
	 * information.
	 *
	 * @invariant analyzer.oclIsKindOf(OFAnalyzer)
	 */
	private IValueAnalyzer analyzer;

	/**
	 * The collection of SCCs in this call graph in bottom-up direction.
	 *
	 * @invariant bottomUpSCC.oclIsKindOf(Sequence(Collection(SootMethod)))
	 */
	private List bottomUpSCC;

	/**
	 * The collection of SCCs in this call graph in top-down direction.
	 *
	 * @invariant topDownSCC.oclIsKindOf(Sequence(Collection(SootMethod)))
	 */
	private List topDownSCC;

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
	 * A comparator to compare call triples based on <code>toString()</code> value of the method being called.
	 *
	 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
	 * @author $Author$
	 * @version $Revision$ $Date$
	 */
	private class CallTripleMethodToStringBasedComparator
	  implements Comparator {
		/**
		 * @see Comparator#compare(Object,Object)
		 */
		public int compare(Object o1, Object o2) {
			return ((CallTriple) o1).getMethod().getSignature().compareTo(((CallTriple) o2).getMethod().getSignature());
		}
	}

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
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getCallees(SootMethod)
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
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getCallees(InvokeExpr,Context)
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
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getCallers(soot.SootMethod)
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
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getHeads()
	 */
	public Collection getHeads() {
		return Collections.unmodifiableCollection(heads);
	}

	/**
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getMethodsReachableFrom(soot.jimple.Stmt,     soot.SootMethod)
	 */
	public Collection getMethodsReachableFrom(final Stmt stmt, final SootMethod root) {
		InvokeExpr ie = stmt.getInvokeExpr();
		Context context = new Context();
		context.setRootMethod(root);

		Collection result = new HashSet();
		Collection callees = getCallees(ie, context);

		for (final Iterator _i = callees.iterator(); _i.hasNext();) {
			result.addAll(getMethodsReachableFrom((SootMethod) _i.next()));
		}
		return result;
	}

	/**
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getMethodsReachableFrom(soot.SootMethod)
	 */
	public Collection getMethodsReachableFrom(final SootMethod root) {
		Collection result = new HashSet(getCallees(root));
		IWorkBag wb = new FIFOWorkBag();

		for (final Iterator _i = result.iterator(); _i.hasNext();) {
			CallTriple _ctrp = (CallTriple) _i.next();
			wb.addWork(_ctrp.getMethod());
		}

		while (wb.hasWork()) {
			SootMethod callee = (SootMethod) wb.getWork();

			if (!result.contains(callee)) {
				Collection callees = CollectionUtils.subtract(getCallees(callee), result);

				for (final Iterator _i = callees.iterator(); _i.hasNext();) {
					final CallTriple _ctrp = (CallTriple) _i.next();
					final SootMethod temp = _ctrp.getMethod();
					wb.addWorkNoDuplicates(temp);
					result.add(temp);
				}
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
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#isReachable(soot.SootMethod)
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
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getReachableMethods()
	 */
	public Collection getReachableMethods() {
		return Collections.unmodifiableCollection(reachables);
	}

	/**
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getSCCs(boolean)
	 */
	public List getSCCs(final boolean topDown) {
		if (topDownSCC == null) {
			topDownSCC = new ArrayList();

			final List _temp = graphCache.getSCCs(true);

			for (final Iterator _i = _temp.iterator(); _i.hasNext();) {
				Collection _scc = (Collection) _i.next();
				final List _l = new ArrayList();

				for (Iterator j = _scc.iterator(); j.hasNext();) {
					_l.add(((SimpleNode) j.next()).getObject());
				}
				topDownSCC.add(Collections.unmodifiableList(_l));
			}
			topDownSCC = Collections.unmodifiableList(topDownSCC);
			bottomUpSCC = new ArrayList(topDownSCC);
			Collections.reverse(bottomUpSCC);
			bottomUpSCC = Collections.unmodifiableList(bottomUpSCC);
		}
		return topDown ? topDownSCC
					   : bottomUpSCC;
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
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.IValueAnalyzerBasedProcessor#callback(ValueBox,Context)
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

					if (!(t instanceof NewExpr || t instanceof StringConstant)) {
						continue;
					}

					SootClass accessClass = null;

					if (invokeExpr instanceof SpecialInvokeExpr && calleeMethod.getName().equals("<init>")) {
						accessClass = calleeMethod.getDeclaringClass();
					} else if (t instanceof NewExpr) {
						NewExpr newExpr = (NewExpr) t;
						accessClass = analyzer.getEnvironment().getClass(newExpr.getBaseType().getClassName());
					} else if (t instanceof StringConstant) {
						accessClass = analyzer.getEnvironment().getClass("java.lang.String");
					}
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
				final Object _head = i.next();
				caller2callees.put(_head, Collections.EMPTY_LIST);
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Starting construction of call graph...");
		}

		// construct call graph 
		graphCache = new SimpleNodeGraph();

		for (Iterator i = reachables.iterator(); i.hasNext();) {
			SootMethod sm = (SootMethod) i.next();
			Collection temp = (Collection) caller2callees.get(sm);
			INode callerNode = graphCache.getNode(sm);

			if (temp != null) {
				for (Iterator j = temp.iterator(); j.hasNext();) {
					CallTriple ctrp = (CallTriple) j.next();
					SootMethod method = ctrp.getMethod();

					graphCache.addEdgeFromTo(callerNode, graphCache.getNode(method));
				}
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

		final List _temp1 = new ArrayList();
		final List _temp2 = new ArrayList();
		_temp1.addAll(caller2callees.keySet());
		Collections.sort(_temp1, ToStringBasedComparator.SINGLETON);

		for (Iterator i = _temp1.iterator(); i.hasNext();) {
			SootMethod caller = (SootMethod) i.next();
			result.append("\n" + caller.getSignature() + "\n");
			_temp2.clear();
			_temp2.addAll((Collection) caller2callees.get(caller));
			Collections.sort(_temp2, new CallTripleMethodToStringBasedComparator());

			for (Iterator j = _temp2.iterator(); j.hasNext();) {
				CallTriple ctrp = (CallTriple) j.next();
				result.append("\t" + ctrp.getMethod().getSignature() + "\n");
			}
		}

		result.append("bottom-up\n");
		_temp1.clear();
		_temp1.addAll(callee2callers.keySet());
		Collections.sort(_temp1, ToStringBasedComparator.SINGLETON);

		for (Iterator i = _temp1.iterator(); i.hasNext();) {
			SootMethod callee = (SootMethod) i.next();
			result.append("\n" + callee.getSignature() + "\n");
			_temp2.clear();
			_temp2.addAll((Collection) callee2callers.get(callee));
			Collections.sort(_temp2, new CallTripleMethodToStringBasedComparator());

			for (Iterator j = _temp2.iterator(); j.hasNext();) {
				CallTriple ctrp = (CallTriple) j.next();
				result.append("\t" + ctrp.getMethod().getSignature() + "\n");
			}
		}

		return result.toString();
	}

	/**
	 * @see edu.ksu.cis.indus.processing.IProcessor#hookup(ProcessingController)
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
	 * @see edu.ksu.cis.indus.processing.IProcessor#unhook(ProcessingController)
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
	final IDirectedGraph getCallGraph() {
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
   Revision 1.53  2004/03/29 01:55:03  venku
   - refactoring.
     - history sensitive work list processing is a common pattern.  This
       has been captured in HistoryAwareXXXXWorkBag classes.
   - We rely on views of CFGs to process the body of the method.  Hence, it is
     required to use a particular view CFG consistently.  This requirement resulted
     in a large change.
   - ripple effect of the above changes.
   Revision 1.52  2004/03/03 02:17:46  venku
   - added a new method to ICallGraphInfo interface.
   - implemented the above method in CallGraph.
   - made aliased use-def call-graph sensitive.
   Revision 1.51  2004/02/25 00:04:02  venku
   - documenation.
   Revision 1.50  2004/01/23 20:13:23  venku
   - removed no-effect code.
   Revision 1.49  2004/01/21 02:52:09  venku
   - the argument to getSCCs was used to create topDownSCC
     rather than just using it to select the ordered SCCs.
   Revision 1.48  2004/01/21 01:34:56  venku
   - logging.
   Revision 1.47  2004/01/20 21:23:36  venku
   - the return value of getSCCs needs to be ordered if
     it accepts a direction parameter.  FIXED.
   Revision 1.46  2004/01/06 01:12:43  venku
   - coding conventions.
   Revision 1.45  2004/01/06 00:17:01  venku
   - Classes pertaining to workbag in package indus.graph were moved
     to indus.structures.
   - indus.structures was renamed to indus.datastructures.
   Revision 1.44  2003/12/31 06:09:34  venku
   - <clinit>s are ignored as heads when the did not
     call any methods.  FIXED.
   Revision 1.43  2003/12/16 00:19:25  venku
   - specialinvoke was handled incorrectly.  FIXED
     It behaves like virtual in cases when a non-instance
     initialization method is invoked.  Otherwise, it acts
     like static invocation. We deal with the first case
     by treating it as virtual invocation and the second
     case as static invoke expr but only with a primary.
   Revision 1.42  2003/12/13 19:38:58  venku
   - removed unnecessary imports.
   Revision 1.41  2003/12/13 02:29:08  venku
   - Refactoring, documentation, coding convention, and
     formatting.
   Revision 1.40  2003/12/09 04:22:10  venku
   - refactoring.  Separated classes into separate packages.
   - ripple effect.
   Revision 1.39  2003/12/08 13:29:48  venku
   - StringConstants were not considered at call-sites.  FIXED.
   Revision 1.38  2003/12/08 12:20:44  venku
   - moved some classes from staticanalyses interface to indus interface package
   - ripple effect.
   Revision 1.37  2003/12/08 12:15:59  venku
   - moved support package from StaticAnalyses to Indus project.
   - ripple effect.
   - Enabled call graph xmlization.
   Revision 1.36  2003/12/07 14:02:45  venku
   MAJOR CHANGE:
    - We previously assumed that there may be parts of the
      system that the flow analysis can suck in.  This is untrue.
      So, we assume all methods marked/tagged via the flow analysis
      as reachables.  All <clinits> and roots as head.
      However, if the flow analysis marks abstract super class methods
      when marking concrete method implementations, then we will
      need to inject back the pruning logic.  As I don't need it
      now I am getting rid of it.
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
    - The above anamoly was fixed by supporting a new class AbstractMutableDirectedGraph.
    - Each Mutable graph extends this graph and exposes itself via
      suitable interface to restrict access.
    - Ripple effect of the above changes.
   Revision 1.11  2003/08/21 03:43:56  venku
   Ripple effect of adding IStatus.
   Revision 1.10  2003/08/17 11:54:25  venku
   Formatting and documentation.
   Revision 1.9  2003/08/17 10:48:34  venku
   Renamed BFA to FA.  Also renamed bfa variables to valueAnalyzer.
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
