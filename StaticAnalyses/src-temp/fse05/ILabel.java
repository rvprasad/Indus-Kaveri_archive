
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

package fse05;

/**
 * DOCUMENT ME!
 * <p></p>
 * 
 * @version $Revision$ 
 * @author <a href="$user_web$">$user_name$</a>
 * @author $Author$
 */
public interface ILabel {
	/** 
	 * <p>DOCUMENT ME! </p>
	 */
	ILabel CALLS = new ILabel() {
			public String toString() {
				return "-CALLS->";
			}
		};

	/** 
	 * <p>DOCUMENT ME! </p>
	 */
	ILabel CD = new ILabel() {
			public String toString() {
				return "-CD->";
			}
		};

	/** 
	 * <p>DOCUMENT ME! </p>
	 */
	ILabel DD = new ILabel() {
			public String toString() {
				return "-DD->";
			}
		};

	/** 
	 * <p>DOCUMENT ME! </p>
	 */
	ILabel EPSILON = new ILabel() {
			public String toString() {
				return "-Epsilon->";
			}
		};
}

// End of File
