
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

package edu.ksu.cis.indus.common.collections;

import java.util.Collection;

import org.apache.commons.collections.Predicate;


/**
 * This predicate checks if a collection is empty.  Hence, this predicate should be used in conjunction with
 * <code>collection</code>  objects.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public final class CollectionEmptinessPredicate
  implements Predicate {
	/** 
	 * The singleton instance of this predicate.
	 */
	public static final CollectionEmptinessPredicate NON_EMPTY_SINGLETON = new CollectionEmptinessPredicate(false);

	/** 
	 * <p>DOCUMENT ME! </p>
	 */
	public static final CollectionEmptinessPredicate EMPTY_SINGLETON = new CollectionEmptinessPredicate(true);

	/** 
	 * <p>DOCUMENT ME! </p>
	 */
	private final boolean emptiness;

	/**
	 * Creates a new CollectionEmptinessPredicate object.
	 *
	 * @param shouldCollectionBeEmpty DOCUMENT ME!
	 */
	private CollectionEmptinessPredicate(final boolean shouldCollectionBeEmpty) {
		emptiness = shouldCollectionBeEmpty;
	}

	/**
	 * @see org.apache.commons.collections.Predicate#evaluate(java.lang.Object)
	 */
	public boolean evaluate(final Object object) {
		assert object instanceof Collection : "This predicate should be used on collection types only.";
		return ((Collection) object).isEmpty() == emptiness;
	}
}

// End of File
