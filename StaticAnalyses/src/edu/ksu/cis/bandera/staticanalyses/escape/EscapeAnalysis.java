
/*
 * Bandera, a Java(TM) analysis and transformation toolkit
 * Copyright (C) 2002, 2003, 2004.
 * Venkatesh Prasad Ranganath (rvprasad@cis.ksu.edu)
 * All rights reserved.
 *
 * This work was done as a project in the SAnToS Laboratory,
 * Department of Computing and Information Sciences, Kansas State
 * University, USA (http://www.cis.ksu.edu/santos/bandera).
 * It is understood that any modification not identified as such is
 * not covered by the preceding statement.
 *
 * This work is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This work is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this toolkit; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 *
 * Java is a trademark of Sun Microsystems, Inc.
 *
 * To submit a bug report, send a comment, or get the latest news on
 * this project and other SAnToS projects, please visit the web-site
 *                http://www.cis.ksu.edu/santos/bandera
 */

package edu.ksu.cis.bandera.staticanalyses.escape;

import ca.mcgill.sable.soot.jimple.NewExpr;
import ca.mcgill.sable.soot.jimple.Stmt;


/**
 * DOCUMENT ME!
 * 
 * <p></p>
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$
 */
public interface EscapeAnalysis {
	/**
	 * DOCUMENT ME!
	 * 
	 * <p></p>
	 *
	 * @param allocSite DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	boolean isMethodEscaping(NewExpr allocSite);

	/**
	 * Checks if the given synchronization statement is executed by a single thread.
	 *
	 * @param stmt is the synchronization statement.
	 *
	 * @return <code>true</code> if <code>stmt</code> is executed by a single thread; <code>false</code>, otherwise.
	 *
	 * @pre stmt.isOclKindOf(ca.mcgill.sable.soot.jimple.ExitMonitorStmt) or
	 * 		stmt.isOclKindOf(ca.mcgill.sable.soot.jimple.EnterMonitorStmt)
	 */
	boolean isSingleThreadSynchronized(Stmt stmt);

	/**
	 * DOCUMENT ME!
	 * 
	 * <p></p>
	 *
	 * @param allocSite DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	boolean isThreadEscaping(NewExpr allocSite);
}

/*****
 ChangeLog:

$Log$

*****/
