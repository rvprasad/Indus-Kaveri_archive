
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

package edu.ksu.cis.indus.staticanalyses.flow.instances.ofa;

import edu.ksu.cis.indus.staticanalyses.flow.AbstractTokenProcessingWork;
import edu.ksu.cis.indus.staticanalyses.flow.IWorkBagProvider;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokenManager;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokens;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This class extends the flow graph node by associating a work peice with it.
 * 
 * <p>
 * Created: Tue Jan 22 04:30:32 2002
 * </p>
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @version $Revision$
 */
class FGAccessNode
  extends OFAFGNode {
	/** 
	 * The logger used by instances of this class to log messages.
	 */
	private static final Log LOGGER = LogFactory.getLog(FGAccessNode.class);

	/** 
	 * The work associated with this node.
	 *
	 * @invariant work != null
	 */
	private final AbstractTokenProcessingWork work;

	/**
	 * Creates a new <code>FGAccessNode</code> instance.
	 *
	 * @param workPeice the work peice associated with this node.
	 * @param provider provides the workbag into which <code>work</code> will be added.
	 * @param tokenManager that manages the tokens used in the enclosing flow analysis.
	 *
	 * @pre workPeice != null and provider != null and tokenManager != null
	 */
	public FGAccessNode(final AbstractTokenProcessingWork workPeice, final IWorkBagProvider provider,
		final ITokenManager tokenManager) {
		super(provider, tokenManager);
		this.work = workPeice;
	}

	/**
	 * Adds the given tokens to the work peice for processing.
	 *
	 * @param newTokens the collection of values that need to be processed at the given node.
	 *
	 * @pre newTokens != null
	 */
	protected void onNewTokens(final ITokens newTokens) {
		super.onNewTokens(newTokens);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Values: " + newTokens + "\nSuccessors: " + succs);
		}
		work.addTokens(newTokens);
		workbagProvider.getWorkBag().addWork(work);
	}
}

// End of File
