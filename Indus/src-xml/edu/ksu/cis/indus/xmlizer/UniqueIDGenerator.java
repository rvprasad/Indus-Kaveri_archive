
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

package edu.ksu.cis.indus.xmlizer;

import soot.Local;
import soot.PatchingChain;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;

import soot.jimple.Stmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * DOCUMENT ME!
 * 
 * <p></p>
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class UniqueIDGenerator
  implements IJimpleIDGenerator {
	/**
	 * <p>
	 * DOCUMENT ME!
	 * </p>
	 */
	private final Map class2fields = new HashMap();

	/**
	 * <p>
	 * DOCUMENT ME!
	 * </p>
	 */
	private final Map method2locals = new HashMap();

	/** 
	 * <p>DOCUMENT ME! </p>
	 */
	private List classes = new ArrayList();

	/**
	 * <p>
	 * DOCUMENT ME!
	 * </p>
	 */
	private List tempList = new ArrayList();

	/**
	 * <p>
	 * DOCUMENT ME!
	 * </p>
	 */
	private int stmtIdCounter;

	/**
	 * <p>
	 * DOCUMENT ME!
	 * </p>
	 */
	private int valueIdCounter = 0;

	/**
	 * @see edu.ksu.cis.indus.xmlizer.IJimpleIDGenerator#getNewClassId()
	 */
	public String getIdForClass(SootClass clazz) {
		if (!classes.contains(clazz)) {
			classes.add(clazz);
		}
		return "c" + String.valueOf(classes.indexOf(clazz));
	}

	/**
	 * @see edu.ksu.cis.indus.xmlizer.IJimpleIDGenerator#getIdForField(soot.SootField)
	 */
	public String getIdForField(SootField field) {
		List fields = (List) class2fields.get(field.getDeclaringClass());
		String result;

		if (fields == null) {
			fields = new ArrayList();
			class2fields.put(field.getDeclaringClass(), fields);
		}

		if (!fields.contains(field)) {
			fields.add(field);
		}
		result = getIdForClass(field.getDeclaringClass()) + "_f" + fields.indexOf(field);
		return result;
	}

	/**
	 * @see edu.ksu.cis.indus.xmlizer.IJimpleIDGenerator#getIdForLocal(soot.Local)
	 */
	public String getIdForLocal(Local v, SootMethod method) {
		List locals = (List) method2locals.get(method);
		String result;

		if (locals == null) {
			locals = new ArrayList();
			method2locals.put(method, locals);
		}

		if (!locals.contains(v)) {
			locals.add(v);
		}
		result = getIdForMethod(method) + "_l" + locals.indexOf(v);
		return result;
	}

	/**
	 * (non-Javadoc)
	 *
	 * @see edu.ksu.cis.indus.xmlizer.IJimpleIDGenerator#getNewMethodId()
	 */
	public String getIdForMethod(SootMethod method) {
		SootClass sc = method.getDeclaringClass();
		return getIdForClass(sc) + "_m" + sc.getMethods().indexOf(method);
	}

	/**
	 * @see edu.ksu.cis.indus.xmlizer.IJimpleIDGenerator#getIdForStmt(soot.jimple.Stmt)
	 */
	public String getIdForStmt(Stmt stmt, SootMethod method) {
		String result = "?";

		if (method.isConcrete()) {
			PatchingChain c = method.getActiveBody().getUnits();
			tempList.clear();
			tempList.addAll(c);
			result = String.valueOf(tempList.indexOf(stmt));
		}
		return getIdForMethod(method) + "_s" + result;
	}

	/**
	 * @see edu.ksu.cis.indus.xmlizer.IJimpleIDGenerator#getIdForType(soot.Type)
	 */
	public String getIdForType(Type type) {
		return type.toString();
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * <p></p>
	 *
	 * @param method DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	public String getNewStmtId(final SootMethod method) {
		return getIdForMethod(method) + "_s" + stmtIdCounter++;
	}

	/**
	 * @see edu.ksu.cis.indus.xmlizer.IJimpleIDGenerator#getNewValueId()
	 */
	public String getNewValueId(final Stmt stmt, final SootMethod method) {
		return getIdForStmt(stmt, method) + "_v" + valueIdCounter++;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * <p></p>
	 */
	public void reset() {
		method2locals.clear();
		class2fields.clear();
		classes.clear();
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * <p></p>
	 */
	public void resetStmtCounter() {
		stmtIdCounter = 0;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * <p></p>
	 */
	public void resetValueCounter() {
		valueIdCounter = 0;
	}
}

/*
   ChangeLog:
   $Log$
 */
