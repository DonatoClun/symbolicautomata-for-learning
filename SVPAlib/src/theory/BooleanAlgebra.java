/**
 * SVPAlib
 * theory
 * Apr 21, 2015
 * @author Loris D'Antoni
 */
package theory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.sat4j.specs.TimeoutException;

import utilities.Pair;

/**
 * BooleanAlgebra over the domain <code>S</code>
 * @param <P> The type of predicates forming the Boolean algebra 
 * @param <S> The domain of the Boolean algebra
 */
public abstract class BooleanAlgebra<P, S> {

	/**
	 * @return the predicate accepting only <code>s</code>
	 * @throws TimeoutException 
	 */
	public abstract P MkAtom(S s);
	
	/**
	 * @return the complement of <code>p</code>
	 * @throws TimeoutException 
	 */
	public abstract P MkNot(P p) throws TimeoutException;

	/**
	 * @return the disjunction of the predicates in <code>pset</code>
	 * @throws TimeoutException 
	 */
	public abstract P MkOr(Collection<P> pset) throws TimeoutException;

	/**
	 * @return the predicate <code>p1</code> or <code>p2</code>
	 * @throws TimeoutException 
	 */
	public abstract P MkOr(P p1, P p2) throws TimeoutException;

	/**
	 * @return the conjunction of the predicates in <code>pset</code>
	 * @throws TimeoutException 
	 */
	public abstract P MkAnd(Collection<P> pset) throws TimeoutException;

	/**
	 * @return the predicate <code>p1</code> and <code>p2</code>
	 * @throws TimeoutException 
	 */
	public abstract P MkAnd(P p1, P p2) throws TimeoutException;

	/**
	 * @return the predicate true
	 */
	public abstract P True();

	/**
	 * @return the predicate false
	 */
	public abstract P False();

	/**
	 * @return the binary predicate true
	 */
	public P binaryTrue() {
		throw new UnsupportedOperationException("binary predicates are not supported by this solver");
	}

	/**
	 * @return the binary predicate false
	 */
	public P binaryFalse() {
		throw new UnsupportedOperationException("binary predicates are not supported by this solver");
	}

	/**
	 * @return transforms the unary predicate into a binary predicate accepting (x,y) in
	 * which x satisfies the given predicate and without constraints on y.
	 */
	public P toBinaryPredicate(P predicate) {
		throw new UnsupportedOperationException("binary predicates are not supported by this solver");
	}

	/**
	 * @return transforms a pair of unary predicate into a binary predicate accepting (x,y) in
	 * which x satisfies the first predicate and y satisfies the second.
	 */
	public P toBinaryPredicate(P predicate1, P predicate2) {
		throw new UnsupportedOperationException("binary predicates are not supported by this solver");
	}

	/**
	 * @return true iff <code>p1</code> and <code>p2</code> are equivalent
	 * @throws TimeoutException 
	 */
	public abstract boolean AreEquivalent(P p1, P p2) throws TimeoutException;

	/**
	 * @return true iff <code>p1</code> is satisfiable
	 */
	public abstract boolean IsSatisfiable(P p1)  throws TimeoutException;

	/**
	 * @return true iff <code>el</code> is a model of <code>p1</code>
	 */
	public abstract boolean HasModel(P p1, S el) throws TimeoutException;

	/**
	 * @return true iff <code>(el1,el2)</code> is a model of a binary predicate <code>p1</code> (used for SVPA)
	 */
	public abstract boolean HasModel(P p1, S el1, S el2) throws TimeoutException;

	/**
	 * @return a witness of the predicate <code>p1</code> if satisfiable, null otherwise
	 */
	public abstract S generateWitness(P p1) throws TimeoutException;

	/**
	 * @return a pair witness of the binary predicate <code>p1</code> if satisfiable, null otherwise
	 */
	public abstract Pair<S, S> generateWitnesses(P p1) throws TimeoutException;

    /**
     * @return true iff there are at least <code>numOfWitnesses</code> many witnesses that satisfy <code>predicate</code>
     */
    public boolean hasNDistinctWitnesses(P predicate, Integer numOfWitnesses) {
        // generate as many witnesses as requested
        for (int witnessID = 0; witnessID < numOfWitnesses; witnessID++) {
            try {
                // generate a witness for the predicate
                S witness = generateWitness(predicate);

                // If it's satisfiable:
                if (witness != null) {
                    // Update the predicate to exclude the current witness.
                    predicate = MkAnd(predicate, MkNot(MkAtom(witness)));
                } else {
                    // If it isn't then we don't have enough witnesses.
                    return false;
                }
            } catch (TimeoutException e) {
                e.printStackTrace();
                System.out.println("Distinct witnesses check timeout.");
                return false;
            }
        }
        // If we get here then we have generated enough witnesses.
        return true;
    }

	/**
	 * Given a set of <code>predicates</code>, returns all the satisfiable
	 * Boolean combinations
	 * 
	 * @return a set of pairs (p,{i1,..,in}) where p is and ij is 0 or 1 base on
	 *         whether pij is used positively or negatively
	 * @throws TimeoutException 
	 */
	public Collection<Pair<P, ArrayList<Integer>>> GetMinterms(
			ArrayList<P> predicates) {
		try {
			return GetMinterms(predicates, True(), Long.MAX_VALUE);
		} catch (TimeoutException e) {			
			e.printStackTrace();
			System.out.println("Minterm construction timeout");
			return null;
		}
	}

	/**
	 * Given a set of <code>predicates</code>, returns all the satisfiable
	 * Boolean combinations
	 * 
	 * @return a set of pairs (p,{i1,..,in}) where p is and ij is 0 or 1 base on
	 *         whether pij is used positively or negatively
	 * @throws TimeoutException 
	 */
	public Collection<Pair<P, ArrayList<Integer>>> GetMinterms(
			ArrayList<P> predicates, long timeout) throws TimeoutException {
		return GetMinterms(predicates, True(), timeout);
	}
	
	private Collection<Pair<P, ArrayList<Integer>>> GetMinterms(
			ArrayList<P> predicates, P startPred, long timeout) throws TimeoutException {
		HashSet<Pair<P, ArrayList<Integer>>> minterms = new HashSet<Pair<P, ArrayList<Integer>>>();
		GetMintermsRec(predicates, 0, startPred, new ArrayList<Integer>(),
				minterms, System.currentTimeMillis(), timeout);
		return minterms;
	}

	private void GetMintermsRec(ArrayList<P> predicates, int n, P currPred,
			ArrayList<Integer> setBits,
			HashSet<Pair<P, ArrayList<Integer>>> minterms, long startime, long timeout) throws TimeoutException {
		
		if(System.currentTimeMillis() - startime > timeout || n>2500)
			throw new TimeoutException("Minterm construction timeout");
			
		if (!IsSatisfiable(currPred))
			return;
		
		if (n == predicates.size())
			minterms.add(new Pair<P, ArrayList<Integer>>(currPred, setBits));
		else {
			ArrayList<Integer> posList = new ArrayList<Integer>(setBits);
			posList.add(1);
			P pn =predicates.get(n);
			GetMintermsRec(predicates, n + 1,
					MkAnd(currPred, pn), posList, minterms, startime, timeout);

			ArrayList<Integer> negList = new ArrayList<Integer>(setBits);
			negList.add(0);
			GetMintermsRec(predicates, n + 1,
					MkAnd(currPred, MkNot(pn)), negList,
					minterms, startime, timeout);
		}
	}
	
	/**
	 * Returns a list of disjoint predicates [p1,...,pn] that has union equal to true that accepts the elements of the predicates [g1...gn] given
	 * as input.
	 */
	public ArrayList<P> GetSeparatingPredicates(
			ArrayList<Collection<S>> characterGroups, long timeout) throws TimeoutException {
		
		//If there is just one bucket return true
		ArrayList<P> out = new ArrayList<>();
		if(characterGroups.size()<=1){
			out.add(True());
			return out;
		}
		
		//Find largest group
		int maxGroup = 0;
		int maxSize = characterGroups.get(0).size();
		for(int i=1;i<characterGroups.size();i++){
			int ithSize = characterGroups.get(i).size();
			if(ithSize>maxSize){
				maxSize=ithSize;
				maxGroup=i;
			}
		}
		
		//Build negated predicate
		P largePred = False(); 
		for(int i=0;i<characterGroups.size();i++){			
			if(i!=maxGroup)
				for(S s: characterGroups.get(i))
					largePred = MkOr(largePred, MkAtom(s));
		}
		largePred = MkNot(largePred);
		
		//Build list of predicates
		for(int i=0;i<characterGroups.size();i++){			
			if(i!=maxGroup){
				P ithPred = False();
				for(S s: characterGroups.get(i))
					ithPred = MkOr(ithPred, MkAtom(s));
				out.add(ithPred);
			}
			else
				out.add(largePred);
		}
		
		return out;		
	}
	
	/**
	 * Returns a list of disjoint predicates [p1,...,pn] that has union equal to true that accepts the elements of the predicates [g1...gn] given
	 * as input. The default method puts all the leftover of the unions of the [g1..gn-1] into gn
	 */
	public ArrayList<P> GetSeparatingPredicatesFromPredicates(
			ArrayList<Collection<P>> predicateGroups, long timeout) throws TimeoutException {
		//If there is just one bucket return true
		ArrayList<P> out = new ArrayList<>();
		if(predicateGroups.size()<=1){
			out.add(True());
			return out;
		}
		
		//Add union each group into corresponding predicate up to n-1 and keeptrack of leftover predicates
		P leftover = True();
		for(int i=0;i<predicateGroups.size()-1;i++){
			P ithPred = False();
			for(P el: predicateGroups.get(i))
				ithPred = MkOr(ithPred, el);
			out.add(ithPred);
			leftover = MkAnd(leftover, MkNot(ithPred));
		}	
		
		out.add(leftover);		
		return out;
	}
}
