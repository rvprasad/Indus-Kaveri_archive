
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

package edu.ksu.cis.indus.tools.slicer;

import java.util.Collection;


/**
 * This interface is used by the slicer tool to retrieve seed slicing criteria.  An implementation of this interface can be
 * used to generate criteria that is based on property of the program points rather than hand-picked by the user.
 *
 * @author <a href="$user_web$">$user_name$</a>
 * @author $Author$
 * @version $Revision$
 */
public interface ISliceCriteriaGenerator {
	/**
	 * Retrieves the slicing criteria.
	 *
	 * @param slicer that uses the criteria.
	 *
	 * @return a collection of criteria.
	 *
	 * @pre slicer != null
	 * @post result != null and result.oclIsKindOf(Collection(ISliceCriterion))
	 */
	Collection getCriteria(final SlicerTool slicer);
}

// End of File
