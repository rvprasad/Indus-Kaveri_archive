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

package edu.ksu.cis.indus.staticanalyses.dependency;

import edu.ksu.cis.indus.annotations.AEmpty;
import edu.ksu.cis.indus.common.datastructures.Pair;

import java.util.Collection;

import soot.Local;
import soot.SootMethod;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;

/**
 * DOCUMENT ME!
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$
 */
public class LocalStmtPairRetriever
		extends AbstractDependenceRetriever<Pair<Stmt, Local>, SootMethod, DefinitionStmt, DefinitionStmt, SootMethod, Stmt> {

	/**
	 * Creates an instance of this class.
	 */
	@AEmpty public LocalStmtPairRetriever() {
		// does nothing
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.IDependenceRetriever#convertToConformantDependees(java.util.Collection,
	 *      java.lang.Object, java.lang.Object)
	 */
	public Collection<Pair<DefinitionStmt, SootMethod>> convertToConformantDependees(final Collection<Stmt> dependees,
			final DefinitionStmt base, final SootMethod context) {
		// TODO: Auto-generated method stub
		return null;
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.IDependenceRetriever#convertToConformantDependents(java.util.Collection,
	 *      java.lang.Object, java.lang.Object)
	 */
	public Collection<Pair<Pair<Stmt, Local>, SootMethod>> convertToConformantDependents(
			final Collection<DefinitionStmt> dependees, final Pair<Stmt, Local> base, final SootMethod context) {

		return null;
	}
}

// End of File