/**
 * SVPAlib
 * @author Loris D'Antoni, Donato Clun
 */
package transducers.psft;

import automata.sfa.SFAInputMove;
import automata.sfa.SFAMove;
import java.io.Serializable;
import org.sat4j.specs.TimeoutException;

import theory.BooleanAlgebra;

/**
 * PSFTInputMove
 * @param <PredicateT> set of predicates over the domain S
 * @param <DomainT> domain of the automaton alphabet
 * @param <OutDomainT> domain of the automaton alphabet
 */
public class PSFTInputMove<PredicateT, DomainT, OutDomainT>
		extends SFAInputMove<PredicateT, DomainT> implements Serializable {

	public final OutDomainT output;

	// only for deserialization
	public PSFTInputMove() {
		super();
		this.output = null;
	}

	public PSFTInputMove(Integer from, Integer to, PredicateT guard, OutDomainT output) {
		super(from, to, guard);
		this.output = output;
	}

	@Override
	public String toString() {
		return String.format("S: %s -%s/%s-> %s", from, guard, output, to);
	}

	@Override
	public String toDotString() {
		return String.format("%s -> %s [label=\"%s/%s\"]\n", from, to, guard, output);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof PSFTInputMove<?, ?, ?>) {
			PSFTInputMove<?, ?, ?> otherCasted = (PSFTInputMove<?, ?, ?>) other;
			return otherCasted.from.equals(from) && otherCasted.to.equals(to)
					&& otherCasted.guard.equals(guard) && otherCasted.output.equals(output);
		}

		return false;
	}

	@Override
	public Object clone(){
		return new PSFTInputMove<PredicateT, DomainT, OutDomainT>(from, to, guard, output);
	}


}
