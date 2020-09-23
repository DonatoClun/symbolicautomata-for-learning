/**
 * SVPAlib
 * automata
 * Apr 21, 2015
 * @author Loris D'Antoni
 */
package automata.sfa;

import java.io.Serializable;
import org.sat4j.specs.TimeoutException;

import theory.BooleanAlgebra;

/**
 * SFAInputMove
 * @param <P> set of predicates over the domain S
 * @param <S> domain of the automaton alphabet
 */
public class SFAInputMove<P,S> extends SFAMove<P, S> implements Serializable {

	public final P guard;

	// only for deserialization
	public SFAInputMove() {
		super();
		guard = null;
	}
	
	/**
	 * Constructs an FSA Transition that starts from state <code>from</code> and ends at state
	 * <code>to</code> with input <code>input</code>
	 */
	public SFAInputMove(Integer from, Integer to, P guard) {
		super(from, to);
		this.guard=guard;
	}
	
	@Override
	public boolean isSatisfiable(BooleanAlgebra<P,S> boolal) throws TimeoutException{
		return boolal.IsSatisfiable(guard);
	}
	
	@Override
	public boolean isDisjointFrom(SFAMove<P,S> t, BooleanAlgebra<P,S> ba) throws TimeoutException{
		if(t.isEpsilonTransition())
			return true;
		if (from.equals(t.from)){			
			SFAInputMove<P, S> ct = (SFAInputMove<P, S>) t;			
			if(ba.IsSatisfiable(ba.MkAnd(guard,ct.guard)))
				return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return String.format("S: %s -%s-> %s",from, guard.toString().trim(), to)
				+ ((additionalAttribute!=null) ? " [" + additionalAttribute + "]": "");
	}

	@Override
	public String toDotString() {
		return String.format("%s -> %s [label=\"%s\"]\n", from, to, guard.toString().trim()
				+ ((additionalAttribute!=null) ? " [" + additionalAttribute + "]": ""));
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof SFAInputMove<?, ?>) {
			SFAInputMove<?, ?> otherCasted = (SFAInputMove<?, ?>) other;
			return otherCasted.from.equals(from) && otherCasted.to.equals(to) && otherCasted.guard.equals(guard);
		}

		return false;
	}

	@Override
	public Object clone(){
		  return new SFAInputMove<P, S>(from,to, guard);
	}

	@Override
	public boolean isEpsilonTransition() {
		return false;
	}

	@Override
	public S getWitness(BooleanAlgebra<P, S> ba) throws TimeoutException {
		return ba.generateWitness(guard);
	}

	@Override
	public boolean hasModel(S el, BooleanAlgebra<P, S> ba) throws TimeoutException {
		return ba.HasModel(guard, el);
	}
	
}
