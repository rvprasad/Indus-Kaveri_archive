
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

package edu.ksu.cis.bandera.staticanalyses.flow.modes.sensitive.flow;

import edu.ksu.cis.bandera.staticanalyses.flow.AbstractIndexManager;
import edu.ksu.cis.bandera.staticanalyses.flow.Context;
import edu.ksu.cis.bandera.staticanalyses.flow.Index;
import edu.ksu.cis.bandera.staticanalyses.flow.modes.sensitive.OneContextInfoIndex;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


// FlowSensitiveIndexManager.java

/**
 * This class manages indices associated with entities in flow sensitive mode.  In reality, it provides the implementation to
 * create new indices.  Created: Tue Mar  5 14:08:18 2002.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @version $Revision$
 */
public class FlowSensitiveIndexManager
  extends AbstractIndexManager {
	/**
	 * An instance of <code>Logger</code> used for logging purpose.
	 */
	private static final Logger LOGGER = LogManager.getLogger(FlowSensitiveIndexManager.class);

	/**
	 * Returns a new instance of this class.
	 *
	 * @return a new instance of this class.
	 */
	public Object prototype() {
		return new FlowSensitiveIndexManager();
	}

	/**
	 * Returns an index corresponding to the given entity and context.  The index is dependent on the program point stored in
	 * the context.
	 *
	 * @param o the entity for which the index in required.
	 * @param c the context which captures program point needed to generate the index.
	 *
	 * @return the index that uniquely identifies <code>o</code> at the program point captured in <code>c</code>.
	 */
	protected Index getIndex(Object o, Context c) {
		LOGGER.debug("Getting index for " + o + " in " + c);

		return new OneContextInfoIndex(o, c.getProgramPoint());
	}
}

/*****
 ChangeLog:

$Log$

*****/
