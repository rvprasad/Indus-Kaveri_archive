
/*
 * Indus, a toolkit to customize and adapt Java programs.
 * Copyright (c) 2002, 2003, 2004, 2005 SAnToS Laboratory, Kansas State University
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

import edu.ksu.cis.indus.interfaces.IPrototype;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootMethod;


/**
 * This class manages of  method variants.  This only provides the implementation to create new method variants.  The super
 * class is responsible of managing the variants.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @version $Revision$
 */
class MethodVariantManager
  extends AbstractVariantManager {
	/** 
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodVariantManager.class);

	/** 
	 * A prototype object used to create index managers related to AST nodes.  Objects created via this prototype object are
	 * used by <code>MethodVariant</code>s to manage the variants corresponding to the AST nodes that exists in them.
	 */
	protected final IPrototype astIMPrototype;

	/** 
	 * The factory used to create method variants.
	 */
	private final IMethodVariantFactory mvFactory;

	/**
	 * Creates a new <code>MethodVariantManager</code> instance.
	 *
	 * @param theAnalysis the instance of the framework in which this object is used.  This parameter cannot be
	 * 		  <code>null</code>.
	 * @param indexManager the manager to indices which are used to map methods to their variants.  This parameter cannot be
	 * 		  <code>null</code>.
	 * @param astIndexManagerPrototype the prototype object used to create index managers related to AST nodes.  This
	 * 		  implementation should support <code>getClone()</code>.
	 * @param factory used to create method variants.
	 *
	 * @pre theAnalysis != null and indexManager != null and astIndexManagerPrototype != null
	 */
	MethodVariantManager(final FA theAnalysis, final IIndexManager indexManager, final IPrototype astIndexManagerPrototype,
		final IMethodVariantFactory factory) {
		super(theAnalysis, indexManager);
		this.astIMPrototype = astIndexManagerPrototype;
		mvFactory = factory;
	}

	/**
	 * Returns a new variant of the method represented by <code>o</code>.
	 *
	 * @param o the method whose variant is to be returned.
	 *
	 * @return the new <code>MethodVariant</code> corresponding to method <code>o</code>.
	 *
	 * @pre o != null and o.oclIsKindOf(SootMethod)
	 * @post result != null
	 */
	protected IVariant getNewVariant(final Object o) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("STATS: Processing method: " + o + "\t number: " + (getVariantCount() + 1));
		}

		final ASTVariantManager _astVM = new ASTVariantManager(fa, (AbstractIndexManager) astIMPrototype.getClone());
		final SootMethod _sootMethod = (SootMethod) o;
		return mvFactory.create(_sootMethod, _astVM, fa);
	}
}

// End of File
