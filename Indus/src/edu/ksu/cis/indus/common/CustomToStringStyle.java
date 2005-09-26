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

package edu.ksu.cis.indus.common;

import org.apache.commons.lang.builder.ToStringStyle;

/**
 * This class customizes the output of <code>toString()</code> by outputting the hashcode at the end instead of in the
 * beginning.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public final class CustomToStringStyle
		extends ToStringStyle {

	/**
	 * This is the singleton object of this class.
	 */
	public static final ToStringStyle HASHCODE_AT_END_STYLE = new CustomToStringStyle();

	/**
	 * The serial version UID.
	 */
	static final long serialVersionUID = -2838615041966055043L;

	// / CLOVER:OFF

	/**
	 * Creates an instance of this class.
	 */
	private CustomToStringStyle() {
		// does nothing
	}

	// / CLOVER:ON

	/**
	 * @see org.apache.commons.lang.builder.ToStringStyle#appendEnd(java.lang.StringBuffer, java.lang.Object)
	 */
	@Override public void appendEnd(final StringBuffer buffer, final Object object) {
		super.appendEnd(buffer, object);
		appendIdentityHashCode(buffer, object);
	}

	/**
	 * @see org.apache.commons.lang.builder.ToStringStyle#appendStart(java.lang.StringBuffer, java.lang.Object)
	 */
	@Override public void appendStart(final StringBuffer buffer, final Object object) {
		appendClassName(buffer, object);
		appendContentStart(buffer);

		if (isFieldSeparatorAtStart()) {
			appendFieldSeparator(buffer);
		}
	}
}

// End of File
