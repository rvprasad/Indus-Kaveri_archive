
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

package edu.ksu.cis.bandera.staticanalyses.flow;


//IPrototype.java

/**
 * <p>
 * This interface helps realize the <i>IPrototype</i> design pattern as defined in the Gang of Four book. It provides the
 * methods via which concrete object can be created from a prototype object.  The default implementation for these methods
 * should raise <code>UnsupportedOperationException</code>.
 * </p>
 *
 * <p>
 * Created: Sun Jan 27 18:04:58 2002
 * </p>
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @version $Revision$
 */
public interface IPrototype {
	/**
	 * <p>
	 * Creates a concrete object from this prototype object.  Usually, it is a duplicate of this prototype object.
	 * </p>
	 *
	 * @return concrete object based on this prototype object.
	 */
	Object prototype();

	/**
	 * <p>
	 * Creates a concrete object from this prototype object.  The concrete object can be parameterized by the information in
	 * <code>o</code>.
	 * </p>
	 *
	 * @param o object containing the information to parameterize the concrete object.
	 *
	 * @return concrete object based on this prototype object.
	 */
	Object prototype(Object o);
}

/*****
 ChangeLog:

$Log$

*****/
