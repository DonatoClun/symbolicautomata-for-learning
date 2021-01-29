/**
 * Equivalence oracle abstract class implementation
 * @author George Argyros
 */
package algebralearning.oracles;

import static java.util.Objects.requireNonNull;

import org.sat4j.specs.TimeoutException;

public interface EquivalenceOracle <P,D> {

	/**
	 * Return a counterexample between the model and the target
	 * or null if the two models are equivalent.
	 * 
	 * @param model The model to compare the target function against
	 * @return A counterexample of type D or null if the two models are equivalent
	 */
	public abstract D getCounterexample(P model) throws TimeoutException;

	/**
	 * Return a counterexample between the model and the target
	 * or null if the two models are equivalent.
	 *
	 * @param model The model to compare the target function against
	 * @return A counterexample of type D or null if the two models are equivalent
	 */
	public abstract Counterexample<D> getCounterexample(P model, long deadline) throws TimeoutException;

	class Counterexample<D>  {
		private final D counterexample;
		public final boolean timeoutExceeded;
		public final boolean hypothesisIsCorrect;

		public Counterexample(D counterexample) {
			requireNonNull(counterexample);
			this.counterexample = counterexample;
			timeoutExceeded = false;
			hypothesisIsCorrect = false;
		}

		public Counterexample(boolean timeoutExceeded, boolean hypothesisIsCorrect) {
			if ((timeoutExceeded && hypothesisIsCorrect) || (!timeoutExceeded && !hypothesisIsCorrect)) {
				throw new RuntimeException("one of the two flags must be true");
			}
			this.timeoutExceeded = timeoutExceeded;
			this.hypothesisIsCorrect = hypothesisIsCorrect;
			counterexample = null;
		}

		public D getCounterexample() {
			if (counterexample == null) {
				throw new RuntimeException("No counterexample present");
			}
			return counterexample;
		}
	}


}
