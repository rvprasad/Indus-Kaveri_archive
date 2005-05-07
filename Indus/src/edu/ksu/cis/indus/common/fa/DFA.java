
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

package edu.ksu.cis.indus.common.fa;

import java.util.Collection;


/**
 * This is an implementation of deterministic finite automaton.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class DFA
  extends NFA {
	/**
	 * Creates an instance of this class.
	 */
	public DFA() {
		super();
	}

	/**
	 * @see edu.ksu.cis.indus.common.fa.IAutomaton#isDeterministic()
	 */
	public boolean isDeterministic() {
		return true;
	}

	/**
	 * @see NFA#addLabelledTransitionFromTo(IState, ITransitionLabel, IState)
	 */
	public void addLabelledTransitionFromTo(final IState src, final ITransitionLabel label, final IState dest) {
		final Collection _states = getResultingStates(src, label);

		if (!_states.isEmpty()) {
			final String _msg = "A transition labelled '" + label + "' already exists from the given source (" + src + ")";
			throw new IllegalStateException(_msg);
		} else if (label.equals(EPSILON)) {
			final String _msg = "Epsilon transitions are not allowed in Deterministic automata.";
			throw new IllegalArgumentException(_msg);
		} else {
			super.addLabelledTransitionFromTo(src, label, dest);
		}
	}
}

// End of File