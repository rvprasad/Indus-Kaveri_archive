
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

package edu.ksu.cis.indus.staticanalyses.flow;

import soot.ValueBox;

import soot.jimple.JimpleValueSwitch;


/**
 * This is the interface to be provided by expression walkers/visitors used in flow analysis.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$
 */
public interface IExprSwitch
  extends JimpleValueSwitch {
	/**
	 * Retreives the result of visiting the object.
	 *
	 * @return the result of visiting the object.
	 */
	Object getResult();

	/**
	 * Processes the expression at the given program point, <code>v</code>.
	 *
	 * @param v the program point at which the to-be-processed expression occurs.
	 *
	 * @pre v != null
	 */
	void process(final ValueBox v);
}

// End of File