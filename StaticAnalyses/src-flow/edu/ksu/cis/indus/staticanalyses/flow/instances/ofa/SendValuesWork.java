
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

package edu.ksu.cis.indus.staticanalyses.flow.instances.ofa;

import edu.ksu.cis.indus.staticanalyses.flow.AbstractWork;
import edu.ksu.cis.indus.staticanalyses.flow.IFGNode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;

import java.util.Collection;


/**
 * This class represents a peice of work to inject a set of values into a flow graph node.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class SendValuesWork
  extends AbstractWork {
	/**
	 * This is the work pool of work peices that will be reused upon request..
	 *
	 * @invariant POOL.borrowObject().oclIsKindOf(SendValuesWork)
	 */
	private static final ObjectPool POOL =
		new SoftReferenceObjectPool(new BasePoolableObjectFactory() {
				/**
				 * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
				 */
				public Object makeObject() {
					return new SendValuesWork();
				}
			});

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Log LOGGER = LogFactory.getLog(SendValuesWork.class);

	/**
	 * Injects the values into the associated node.
	 */
	public final void execute() {
		node.addValues(values);
	}

	/**
	 * Puts back this work into the work pool to be reused.
	 *
	 * @throws RuntimeException if the return of the object to the pool failed.
	 */
	protected void finished() {
		try {
			POOL.returnObject(this);
		} catch (Exception e) {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("How can this happen?", e);
			}
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new <code>SendValuesWork</code> instance.
	 *
	 * @param toNode the node into which the values need to be injected.
	 * @param valueToBeSent the value to be injected.
	 *
	 * @return a work peice with the given data embedded in it.
	 *
	 * @post result != null
	 * @pre toNode != null and valueToBeSent != null
	 */
	static final SendValuesWork getWork(final IFGNode toNode, final Object valueToBeSent) {
		SendValuesWork result = getWork();

		result.setFGNode(toNode);
		result.addValue(valueToBeSent);
		return result;
	}

	/**
	 * Creates a new <code>SendValuesWork</code> instance.
	 *
	 * @param toNode the node into which the values need to be injected.
	 * @param valuesToBeSent a collection containing the values to be injected.
	 *
	 * @return a work peice with the given data embedded in it.
	 *
	 * @post result != null
	 * @pre toNode != null and valuesToBeSent != null
	 */
	static final SendValuesWork getWork(final IFGNode toNode, final Collection valuesToBeSent) {
		SendValuesWork result = getWork();
		result.setFGNode(toNode);
		result.addValues(valuesToBeSent);
		return result;
	}

	/**
	 * Returns a work peice from the pool.
	 *
	 * @return the work peice
	 *
	 * @throws RuntimeException if the work peice could not be retrieved.
	 */
	private static SendValuesWork getWork() {
		SendValuesWork result;

		try {
			result = (SendValuesWork) POOL.borrowObject();
			result.values.clear();
		} catch (Exception e) {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("How can this happen?", e);
			}
			throw new RuntimeException(e);
		}
		return result;
	}
}

/*
   ChangeLog:
   $Log$
   Revision 1.6  2003/08/21 12:43:28  venku
   Previous values were not cleared when the work peice was retrieved from the pool - FIXED.
   Revision 1.5  2003/08/18 11:07:46  venku
   Name change for pooling support.
   Revision 1.4  2003/08/18 08:34:20  venku
   Well, used the object pool as available from jakarta commons implementation.
   Revision 1.3  2003/08/18 07:09:13  venku
   The way objects were removed from the pool was incorrect.  I had used get() instead of remove().
   This will not remove the object from the pool.  This has been fixed.
   Revision 1.2  2003/08/18 01:01:18  venku
   Trying to fix CVS's erratic behavior.
   Revision 1.1  2003/08/17 11:19:13  venku
   Placed the simple SendValuesWork class into a separate file.
   Extended it with work pool support.
   Amended AbstractWork and WorkList to enable work pool support.
 */
