
/*
 * Indus, a toolkit to customize and adapt Java programs.
 * Copyright (c) 2003 SAnToS Laboratory, Kansas State University
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

import edu.ksu.cis.indus.tools.ToolConfigurator;


/**
 * This provides the graphical user interface via which the user can configure the slicer.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class SlicerConfigurator
  implements ToolConfigurator {
	/**
	 * The configurationCollection which can be edited via this editor.
	 */
	private final SlicerConfigurationCollection configuration;

	/**
	 * Creates a new SlicerConfigurator object.
	 *
	 * @param config is the configurationCollection that can be edited by this editor.
	 */
	SlicerConfigurator(final SlicerConfigurationCollection config) {
		configuration = config;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see edu.ksu.cis.indus.tools.ToolConfigurator#display()
	 */
	public void display(Object something) {
		// TODO: Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see edu.ksu.cis.indus.tools.ToolConfigurator#dispose()
	 */
	public void dispose() {
		// TODO: Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see edu.ksu.cis.indus.tools.ToolConfigurator#hide()
	 */
	public void hide() {
		// TODO: Auto-generated method stub
	}
}

/*
   ChangeLog:
   $Log$
   Revision 1.2  2003/09/26 05:55:28  venku
   - a checkpoint commit. Also a cvs fix commit.

   Revision 1.1  2003/09/24 07:32:23  venku
   - Created an implementation of indus tool api specific to Slicer.
     The GUI needs to be setup and bandera adapter needs to be fixed.
 */
