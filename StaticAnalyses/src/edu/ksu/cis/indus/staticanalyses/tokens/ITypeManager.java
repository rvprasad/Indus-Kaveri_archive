
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

package edu.ksu.cis.indus.staticanalyses.tokens;

import java.util.Collection;

import soot.Type;


/**
 * This is the interface to a type manager.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public interface ITypeManager {
	/**
	 * Retrieves all the types of the given value.  Depending on the type system, <code>value</code> may be of many types
	 * (for example, due to subtyping).  Hence, we retrieve a collection of types rather than a type.
	 *
	 * @param value whose types are requested.
	 *
	 * @return the types of <code>value</code>.
	 *
	 * @pre value != null
	 * @post result != null
	 * @post result->forall(o | o.oclIsKindOf(IType))
	 */
	Collection getAllTypes(Object value);

	/**
	 * Retrieves the specific type of the value.  It may be that the value is of type T1, T2, .. Tn of which Tn is it's
	 * declared type and the rest are types by some sort of subtyping relation.  This method should return Tn.
	 *
	 * @param value whose type is requested.
	 *
	 * @return the type of the value.
	 *
	 * @pre value != null
	 * @post result != null
	 */
	IType getExactType(Object value);

	/**
	 * Retrieves a type for the given IR type.  The user may use a type system orthogonal to the type system provided by the
	 * intermediate representation used to represent the system.  In such cases, to decouple the IR type system from the
	 * user's type system,
	 *
	 * @param type is the intermediate representation type.
	 *
	 * @return the type based on the type system represented by this type manager.
	 *
	 * @pre type != null
	 * @post result != null
	 */
	IType getTypeForIRType(Type type);

	/**
	 * Resets the type manager.
	 */
	void reset();
}

// End of File
