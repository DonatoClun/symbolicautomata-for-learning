/**
 * Equivalence oracle instantiation for the SFA algebra.
 * @author George Argyros
 */

package algebralearning.sfa;

import algebralearning.oracles.EquivalenceOracle;
import automata.sfa.SFA;
import org.sat4j.specs.TimeoutException;
import theory.BooleanAlgebra;
import utilities.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 * Implements the equivalence oracle for SFAs. 
 * 
 * @param <P> The type of predicates in the target SFA. 
 * @param <D> The domain of the algebra of the target SFA.
 */
public class SFAEquivalenceOracle <P,D> implements EquivalenceOracle <SFA <P,D>, List <D>> {

	private SFA <P,D> correctModel;
	private BooleanAlgebra <P,D> ba;
	private List <D> cachedCe; 	
	private Integer distinctCeNum;
	private Integer cachedCeNum;
	
	/**
	 * 
	 * @param cModel the target SFA for which equivalence will be checked against.
	 * @param b the boolean algebra used by the cModel SFA.
	 */
	public SFAEquivalenceOracle(SFA <P,D> cModel, BooleanAlgebra <P,D> b) {		
		correctModel = cModel;
		ba = b;
		distinctCeNum = 0;
		cachedCeNum = 0;
		// cachedCe contains the last counterexample and the model is checked against it before 
		// invoking the more expensive equivalence test. 
		cachedCe = null;
	}
    
	
    public List <D>getCounterexample (SFA <P,D> model) throws TimeoutException {    		
		if ((cachedCe != null) && 
				(model.accepts(cachedCe, ba) != correctModel.accepts(cachedCe, ba))) {
			cachedCeNum ++;
			return cachedCe;
		}    			    		
		Pair <Boolean, List<D>>p = correctModel.isEquivalentPlusWitnessTo(model, ba);
		if (p.first) {    			
			return null;
		} else {    			
			cachedCe = new LinkedList <D>(p.second);
			distinctCeNum ++;
			return p.second;
		}
    }

	@Override
	public Counterexample<List<D>> getCounterexample(SFA<P, D> model, long deadline)
			throws TimeoutException {
		throw new RuntimeException("Not implemented.");
	}

	/**
     * @return the number of cached counterexamples used so far.
     */
    	public Integer getCachedCeNum() {
		   return cachedCeNum; 		
    	}
    	
    	/**
    	 * @return the number of distinct counterexamples used so far.
    	 */
    	public Integer getDistinctCeNum() {
 		   return distinctCeNum; 		
 	}    	
}
