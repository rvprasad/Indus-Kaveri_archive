package edu.ksu.cis.bandera.bfa.analysis.ofa;


import edu.ksu.cis.bandera.bfa.BFA;
import edu.ksu.cis.bandera.bfa.Context;
import edu.ksu.cis.bandera.bfa.FGNode;
import edu.ksu.cis.bandera.bfa.FGNodeConnector;
import edu.ksu.cis.bandera.bfa.MethodVariant;

import ca.mcgill.sable.soot.SootField;
import ca.mcgill.sable.soot.jimple.FieldRef;
import ca.mcgill.sable.soot.jimple.NullConstant;
import ca.mcgill.sable.soot.jimple.Value;

import java.util.Iterator;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

// FieldAccessExprWork.java
/**
 * <p>This class encapsulates the logic to instrument the flow of values corresponding to fields.</p>
 *
 * Created: Wed Mar  6 03:32:30 2002.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @version $Revision$
 */

public class FieldAccessExprWork extends  AbstractAccessExprWork {

	/**
	 * <p>An instance of <code>Logger</code> used for logging purpose.</p>
	 *
	 */
	private static final Logger logger = LogManager.getLogger(FieldAccessExprWork.class);

	/**
	 * <p>The ast flow graph node which needs to be connected to non-ast nodes depending on the values that occur at the
	 * primary.</p>
	 *
	 */
	protected final FGNode ast;

	/**
	 * <p>The connector to be used to connect the ast and non-ast node.</p>
	 *
	 */
	protected final FGNodeConnector connector;

	/**
	 * <p>Creates a new <code>FieldAccessExprWork</code> instance.</p>
	 *
	 * @param caller the method in which the access occurs.
	 * @param accessExpr the field access expression.
	 * @param context the context in which the access occurs.
	 * @param ast the flow graph node associated with the access expression.
	 * @param connector the connector to use to connect the ast node to the non-ast node.
	 */
	public FieldAccessExprWork (MethodVariant caller, FieldRef accessExpr, Context context, FGNode ast,
								FGNodeConnector connector) {
		super(caller, accessExpr, context);
		this.ast = ast;
		this.connector = connector;
	}

	/**
	 * <p>Connects non-ast nodes to ast nodes when new values arrive at the primary of the field access expression.</p>
	 *
	 */
	public synchronized void execute() {
		SootField sf = ((FieldRef)accessExpr).getField();
		BFA bfa = caller.bfa;
		logger.debug(values + " values arrived at base node of " + accessExpr);
		for (Iterator i = values.iterator(); i.hasNext();) {
			 Value v = (Value)i.next();

			 if (v instanceof NullConstant) {
				 continue;
			 } // end of if (v instanceof NullConstant)

			 context.setAllocationSite(v);
			 FGNode nonast = bfa.getFieldVariant(sf, context).getFGNode();
			 connector.connect(ast, nonast);
		} // end of for (Iterator i = values.iterator(); i.hasNext();)
	}

}// FieldAccessExprWork
