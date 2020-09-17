/**
 * @author Loris D'Antoni, Donato clun
 */
package transducers.psft;

import automata.Move;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.Map.Entry;
import java.util.Set;
import logic.ltl.Predicate;
import org.sat4j.specs.TimeoutException;

import theory.BooleanAlgebra;
import utilities.Pair;
import utils.VertexColoring;

/**
 * Partially symbolic finite transducer. Each transition
 * has exactly one element of the output domain.
 *
 * @param <PredicateT>
 *            set of predicates over the domain DomainT
 * @param <DomainT>
 *            domain of the automaton alphabet
 * @param <OutDomainT>
 *            output domain
 */
public class PSFT<PredicateT, DomainT, OutDomainT> //extends Automaton<PredicateT, DomainT>
		implements Serializable {
	// ------------------------------------------------------
	// Automata properties
	// ------------------------------------------------------

	private Integer initialState;
	private final Collection<Integer> states;
	private final Collection<Integer> finalStates;
	private boolean isTotal;

	private final Map<Integer, Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>>> inputMovesFrom;
	private final Map<Integer, Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>>> inputMovesTo;

	private Integer maxStateId;
	private Integer transitionCount;

	/**
	 * @return number of states in the automaton
	 */
	public Integer stateCount() {
		return states.size();
	}

	/**
	 * @return number of transitions in the automaton
	 */
	public Integer getTransitionCount() {
		return transitionCount;
	}

	// ------------------------------------------------------
	// Constructors
	// ------------------------------------------------------

	// Initializes all the fields of the automaton
	private PSFT() {
		super();
		finalStates = new HashSet<>();
		states = new HashSet<>();
		inputMovesFrom = new HashMap<>();
		inputMovesTo = new HashMap<>();
		transitionCount = 0;
		maxStateId = 0;
	}

	private PSFT(Collection<Integer> finalStates, Collection<Integer> states,
			Map<Integer, Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>>> inputMovesFrom,
			Map<Integer, Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>>> inputMovesTo,
			Integer transitionCount,
			Integer maxStateId) {
		super();
		this.finalStates = finalStates;
		this.states = states;
		this.inputMovesFrom = inputMovesFrom;
		this.inputMovesTo = inputMovesTo;
		this.transitionCount = transitionCount;
		this.maxStateId = maxStateId;
	}

	public boolean accepts(List<DomainT> input, BooleanAlgebra<PredicateT, DomainT> ba)
			throws TimeoutException {
		Integer state = getInitialState();

		nextChar:
		for (DomainT el : input) {
			Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> movesFrom = getInputMovesFrom(
					state);
			for (PSFTInputMove<PredicateT, DomainT, OutDomainT> move : movesFrom) {
				if (ba.HasModel(move.guard, el)) {
					state = move.to;
					continue nextChar;
				}
			}
			return false;
		}

		return finalStates.contains(state);
	}

	public List<OutDomainT> translate(BooleanAlgebra<PredicateT, DomainT> ba, DomainT ... input)
			throws TimeoutException {
		return translate(Arrays.asList(input), ba);
	}

	public PSFTInputMove<PredicateT, DomainT, OutDomainT> getInputMoveFrom(
			Integer state, DomainT input, BooleanAlgebra<PredicateT, DomainT> ba) {
		for (PSFTInputMove<PredicateT, DomainT, OutDomainT> move : getInputMovesFrom(state)) {
			try {
				if (ba.HasModel(move.guard, input)) {
					return move;
				}
			} catch (TimeoutException e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	public List<OutDomainT> translate(List<DomainT> input, BooleanAlgebra<PredicateT, DomainT> ba) {
			Integer state = getInitialState();
			ArrayList<OutDomainT> result = new ArrayList<>(input.size());

			boolean error = false;

			for (DomainT el : input) {
				if (!error) {
					PSFTInputMove<PredicateT, DomainT, OutDomainT> move = getInputMoveFrom(state, el, ba);
					if (move != null) {
						result.add(move.output);
						state = move.to;
						continue;
					}
				}
				result.add(null);
				error = true;
			}

			return result;
	}


	public void addState(Integer stateId) {
		inputMovesFrom.computeIfAbsent(stateId, k -> new HashSet<>());
		inputMovesTo.computeIfAbsent(stateId, k -> new HashSet<>());
		states.add(stateId);
	}

	public void addAllStates(Collection<Integer> states) {
		for (Integer state : states) {
			addState(state);
		}
	}

	/**
	 * Create an automaton and removes unreachable states
	 *
	 * @throws TimeoutException
	 */
	public static <A, B, C> PSFT<A, B, C> MkPSFT(Collection<PSFTInputMove<A, B, C>> transitions, Integer initialState,
			Collection<Integer> finalStates, BooleanAlgebra<A, B> ba) throws TimeoutException {

		return MkPSFT(transitions, initialState, finalStates, ba, true);
	}


	/**
	 * Create an automaton and removes unreachable states and only removes
	 * unreachable states if <code>remUnreachableStates<code> is true
	 *
	 * @throws TimeoutException
	 */
	public static <A, B, C> PSFT<A, B, C> MkPSFT(Collection<PSFTInputMove<A, B, C>> transitions, Integer initialState,
			Collection<Integer> finalStates, BooleanAlgebra<A, B> ba, boolean remUnreachableStates)
			throws TimeoutException {

		PSFT<A, B, C> aut = new PSFT<>();

		aut.addState(initialState);
		aut.addAllStates(finalStates);

		aut.initialState = initialState;
		aut.finalStates.addAll(finalStates);

		for (PSFTInputMove<A, B, C> t : transitions)
			aut.addTransition(t, ba);

		if (remUnreachableStates)
			aut = removeDeadOrUnreachableStates(aut, ba);

		return aut;
	}

	private void addTransition(PSFTInputMove<PredicateT, DomainT, OutDomainT> transition,
			BooleanAlgebra<PredicateT, DomainT> ba) {

		Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> inputMovesFrom = getInputMovesFrom(
				transition.from);

		try {
			for (PSFTInputMove<PredicateT, DomainT, OutDomainT> move : inputMovesFrom) {
				if (ba.IsSatisfiable(ba.MkAnd(move.guard, transition.guard))) {
					throw new RuntimeException("adding the specified transition would make the PSFT nondet");
				}

				if (transition.to.equals(move.to) &&transition.output != null &&
						transition.output.equals(move.output)) {
					throw new RuntimeException("there is already a transition with "
							+ "same source, target and output");
				}
			}
		} catch (TimeoutException e) {
			throw new RuntimeException(e);
		}

		transitionCount++;
		if (transition.from > maxStateId)
			maxStateId = transition.from;
		if (transition.to > maxStateId)
			maxStateId = transition.to;
		addState(transition.from);
		addState(transition.to);
		inputMovesFrom.add(transition);
		getInputMovesTo(transition.to).add(transition);
	}

	public PSFT<PredicateT, DomainT, OutDomainT> minimize(BooleanAlgebra<PredicateT, DomainT> ba, Set<Integer> sinkStates) throws TimeoutException {
		final PredicateT pFalse = ba.False();
		HashMap<Integer, OutputFunction> outDomainPredicates =
				getOutputFunctions(ba);
		List<Integer> sortedStates = new ArrayList<>(getStates());
		sortedStates.sort(Integer::compareTo);
		int nStates = sortedStates.size();

		// Compute minterms
		ArrayList<PredicateT> minTerms = getMinTerms(ba);

		boolean[][] conflictsMap = computeConflicts(ba, sortedStates, outDomainPredicates);

		start:
		while (true) {
			for (int nA = 0; nA < nStates; nA++) {
				for (int nB = 0; nB < nStates; nB++) {
					if (conflictsMap[nA][nB]) {
						// if there is already a conflict, skip
						continue;
					}
					Integer A = sortedStates.get(nA);
					Integer B = sortedStates.get(nB);
					for (PredicateT minTerm : minTerms) {
						DomainT wit = ba.generateWitness(minTerm);
						Integer A_wit_target = getInputMoveFrom(A, wit, ba).to;
						Integer B_wit_target = getInputMoveFrom(B, wit, ba).to;

						int nA_wit_target = sortedStates.indexOf(A_wit_target);
						int nB_wit_target = sortedStates.indexOf(B_wit_target);

						if (conflictsMap[nA_wit_target][nB_wit_target]) {
							// if there is a conflict in the target pair, add a conflict in the source pair
							conflictsMap[nA][nB] = true;
							conflictsMap[nB][nA] = true;
							continue start;
						}
					}
				}
			}
			break;
		}


		int[] vertexColors = new int[nStates];
		int numColors = VertexColoring.vertexColoring(conflictsMap, vertexColors);
		List<HashSet<Integer>> partition = new ArrayList<>(numColors);
		for (int i = 0; i < numColors; i++) {
			partition.add(new HashSet<>());
		}
		for (int n = 0; n < nStates; n++) {
			Integer stateId = sortedStates.get(n);
			int group = vertexColors[n];
			partition.get(group).add(stateId);
		}

		HashMap<Integer, Integer> partitionIndex = computePartitionIndex(partition);

		Integer initialState = partitionIndex.get(getInitialState());

		HashMap<Integer, HashMap<Integer, HashMap<OutDomainT, PredicateT>>> transitions_from = new HashMap<>();

		StringBuilder log = new StringBuilder();
		boolean err = false;

		for (Integer original_source : getStates()) {
			Integer minimized_source = partitionIndex.get(original_source);
			HashMap<Integer, HashMap<OutDomainT, PredicateT>> transitions_fromMinimizedSource
					= transitions_from.computeIfAbsent(minimized_source, k -> new HashMap<>());

			nextMove:
			for (PSFTInputMove<PredicateT, DomainT, OutDomainT> originalMove :
					getInputMovesFrom(original_source)) {
				log.append("SRC TRAN ").append(originalMove.toString()).append(": \n");
				if (originalMove.output == null) {
//					throw new RuntimeException("???");
					continue;
				}

				Integer minimized_target = partitionIndex.get(originalMove.to);

				for (Entry<Integer, HashMap<OutDomainT, PredicateT>> transition_fromMinimizedSource :
						transitions_fromMinimizedSource.entrySet()) {
					Integer transition_fromMinimizedSource_minimizedDest
							= transition_fromMinimizedSource.getKey();
					HashMap<OutDomainT, PredicateT> transition_fromMinimizedSource_outPredMap
							= transition_fromMinimizedSource.getValue();
					for (Entry<OutDomainT, PredicateT> entry :
							transition_fromMinimizedSource_outPredMap.entrySet()) {
						OutDomainT out = entry.getKey();
						PredicateT pred = entry.getValue();
						if (!transition_fromMinimizedSource_minimizedDest.equals(minimized_target)
								&& ba.IsSatisfiable(ba.MkAnd(pred, originalMove.guard))) {
							continue nextMove;
//							err = true;
						}
					}
				}

				HashMap<OutDomainT, PredicateT> transitions_from_to_output
						= transitions_fromMinimizedSource.computeIfAbsent(minimized_target, k -> new HashMap<>());

				PredicateT oldPred = transitions_from_to_output.computeIfAbsent(originalMove.output, k -> pFalse);
				PredicateT newPred = ba.MkOr(oldPred, originalMove.guard);
				transitions_from_to_output.put(originalMove.output, newPred);
				log.append("MIN_FROM=").append(minimized_source).append("\n");
				log.append("MIN_TO=").append(minimized_target).append("\n");
				log.append("OUT=").append(originalMove.output).append("\n");
				log.append("OLDPRED=").append(oldPred.toString())
						.append(" NEWPRED=").append(newPred.toString()).append("\n\n");


			}
		}

//		if (err) {
//			throw new RuntimeException("nondet");
//		}

		Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> transitions = new ArrayList<>();

		transitions_from.forEach((from, dict) ->
				dict.forEach((to, dict1) ->
						dict1.forEach((out, pred) ->
								transitions.add(new PSFTInputMove<>(from, to, pred, out)))));


		String logstr = log.toString();
		PSFT<PredicateT, DomainT, OutDomainT> res = MkPSFT(transitions, initialState, Collections.EMPTY_LIST, ba, false);

		return res;
	}

	public ArrayList<PredicateT> getMinTerms(BooleanAlgebra<PredicateT, DomainT> ba) {
		ArrayList<PredicateT> predicates = new ArrayList<>();
		inputMovesFrom.forEach((key, value) -> value.forEach(t -> predicates.add(t.guard)));
		ArrayList<PredicateT> minTerms = new ArrayList<>();
		ba.GetMinterms(predicates).forEach(p -> minTerms.add(p.first));
		return minTerms;
	}

	public void minimizeOld(BooleanAlgebra<PredicateT, DomainT> ba, Set<Integer> sinkStates) throws TimeoutException {
		HashMap<Integer, OutputFunction> outDomainPredicates =
				getOutputFunctions(ba);
		List<Integer> sortedStates = new ArrayList<>(getStates());
		sortedStates.sort(Integer::compareTo);
		int nStates = sortedStates.size();

		boolean[][] conflictsMap = computeConflicts(ba, sortedStates, outDomainPredicates);
		int[] vertexColors = new int[nStates];
		int numColors = VertexColoring.vertexColoring(conflictsMap, vertexColors);
		List<HashSet<Integer>> partition = new ArrayList<>(numColors);
		for (int i = 0; i < numColors; i++) {
			partition.add(new HashSet<>());
		}
		for (int n = 0; n < nStates; n++) {
			Integer stateId = sortedStates.get(n);
			if (!sinkStates.contains(stateId)) {
				int group = vertexColors[n];
				partition.get(group).add(stateId);
			}
		}

		Integer sinkStateSet = null;
		if (sinkStates.size() > 0) {
			sinkStateSet = partition.size();
			partition.add(new HashSet<>(sinkStates));
		}

		// Compute minterms
		ArrayList<PredicateT> minTerms = getMinTerms(ba);

		start:
		while (true) {
			HashMap<Integer, Integer> partitionIndex = computePartitionIndex(partition);
			for (int partitionSetId = 0; partitionSetId < partition.size(); partitionSetId++) {
				HashSet<Integer> partitionSet = partition.get(partitionSetId);
				for (PredicateT minTerm : minTerms) {
					DomainT w = ba.generateWitness(minTerm);
					HashMap<Integer, HashSet<Integer>> targetPartitionSets = new HashMap<>();
					for (Integer stateId : partitionSet) {
						PSFTInputMove<?, ?, ?> inputMoveFrom = getInputMoveFrom(stateId, w, ba);
						if (inputMoveFrom != null) {
							Integer targetState = inputMoveFrom.to;
							Integer targetStateGroup = partitionIndex.get(targetState);
							HashSet<Integer> states = targetPartitionSets
									.computeIfAbsent(targetStateGroup, k -> new HashSet<>());
							states.add(stateId);
						}
					}
					// check if they all lead to the same group
					if (sinkStateSet!=null) {
						targetPartitionSets.remove(sinkStateSet);
					}

					if (targetPartitionSets.size() > 1) {
						//this group must be split
						partition.remove(partitionSet);
						targetPartitionSets.forEach((g,s)->partition.add(s));
						continue start;
					}
				}
			}
			break;
		}

	}

	private HashMap<Integer, Integer> computePartitionIndex(List<HashSet<Integer>> partition) {
		HashMap<Integer, Integer> partitionIndex = new HashMap<>();
		for (int groupId = 0; groupId < partition.size(); groupId++) {
			for (Integer stateId : partition.get(groupId)) {
				partitionIndex.put(stateId, groupId);
			}
		}
		return partitionIndex;
	}


	private boolean[][] computeConflicts(BooleanAlgebra<PredicateT, DomainT> ba,
			List<Integer> sortedStates,
			HashMap<Integer, OutputFunction> outDomainPredicates)
			throws TimeoutException {
		boolean[][] conflictsMap;
		int nStates = sortedStates.size();
		conflictsMap = new boolean[nStates][nStates];
		for (int nA = 0; nA < nStates; nA++) {
			for (int nB = nA + 1; nB < nStates; nB++) {
				Integer A = sortedStates.get(nA);
				Integer B = sortedStates.get(nB);
				OutputFunction funA = outDomainPredicates.get(A);
				OutputFunction funB = outDomainPredicates.get(B);
				if (funA.conflictWith(funB)) {
					conflictsMap[nA][nB] = true;
					conflictsMap[nB][nA] = true;
				}
			}
		}
		return conflictsMap;
	}

	private HashMap<Integer, OutputFunction> getOutputFunctions (
			BooleanAlgebra<PredicateT, DomainT> ba) throws TimeoutException {

		HashMap<Integer, OutputFunction> oresult = new HashMap<>();

		for(Integer state: getStates()) {
			OutputFunction of = new OutputFunction(ba);
			getInputMovesFrom(state).forEach(m -> of.set(m.guard, m.output));
			oresult.put(state, of);
		}

		return oresult;
	}

	private class OutputFunction {
		OutputFunction(BooleanAlgebra<PredicateT, DomainT> ba) {
			this.ba = ba;
		}
		final BooleanAlgebra<PredicateT, DomainT> ba;

		HashMap<OutDomainT, PredicateT> data = new HashMap<>();

		void set(PredicateT pred, OutDomainT out) {
			try {
				data.putIfAbsent(out, ba.False());
				data.put(out, ba.MkOr(data.get(out), pred));
			} catch (TimeoutException e) {
				throw new RuntimeException(e);
			}
		}

		boolean conflictWith(OutputFunction other) {
			try {
				for (OutDomainT thisOutputValue : data.keySet()) {
					if (thisOutputValue == null) {
						continue;
					}
					PredicateT thisPredicate = data.get(thisOutputValue);
					for (OutDomainT otherOutputValue : other.data.keySet()) {
						if (otherOutputValue == null || thisOutputValue.equals(otherOutputValue)) {
							continue;
						}
						PredicateT otherPredicate = other.data.get(otherOutputValue);
						if (ba.IsSatisfiable(ba.MkAnd(thisPredicate, otherPredicate))) {
							return true;
						}
					}
				}
				return false;
			} catch (TimeoutException te) {
				throw new RuntimeException(te);
			}
		}

	}

	// ------------------------------------------------------
	// Other automata operations
	// ------------------------------------------------------

	/**
	 * @return a new total equivalent total PSFT (with one transition for each
	 *         symbol out of every state)
	 * @throws TimeoutException
	 */
	public PSFT<PredicateT, DomainT, OutDomainT> mkTotal(BooleanAlgebra<PredicateT, DomainT> ba) throws TimeoutException {
		return mkTotal(this, ba);
	}

	/**
	 * @return a new total total PSFT (with one transition for each symbol out of
	 *         every state) equivalent to <code>aut</code>
	 * @throws TimeoutException
	 */
	@SuppressWarnings("unchecked")
	public static <A, B, C> PSFT<A, B, C> mkTotal(PSFT<A, B, C> aut, BooleanAlgebra<A, B> ba)
			throws TimeoutException {

		if (aut.isTotal) {
			return (PSFT<A, B, C>) aut.clone();
		}

		Collection<PSFTInputMove<A, B, C>> transitions = new ArrayList<>();
		Integer initialState = aut.initialState;
		Collection<Integer> finalStates = new HashSet<>(aut.finalStates);

		int sinkState = aut.maxStateId + 1;
		boolean addSink = false;
		for (Integer state : aut.states) {

			A totGuard = null;
			for (PSFTInputMove<A, B, C> move : aut.getInputMovesFrom(state)) {
				transitions.add(move);
				if (totGuard == null)
					totGuard = ba.MkNot(move.guard);
				else
					totGuard = ba.MkAnd(totGuard, ba.MkNot(move.guard));
			}
			// If there are not transitions out of the state set the guard to
			// the sink to true
			if (totGuard == null)
				totGuard = ba.True();
			if (ba.IsSatisfiable(totGuard)) {
				addSink = true;
				transitions.add(new PSFTInputMove<>(state, sinkState, totGuard, null));
			}
		}
		if (addSink)
			transitions.add(new PSFTInputMove<>(sinkState, sinkState, ba.True(), null));

		// Do not remove unreachable states otherwise the sink will be removed
		// again
		PSFT<A, B, C> result = MkPSFT(transitions, initialState, finalStates, ba, false);
		result.isTotal = true;
		return result;
	}

	private static <A, B, C> PSFT<A, B, C> removeDeadOrUnreachableStates(PSFT<A, B, C> aut, BooleanAlgebra<A, B> ba)
			throws TimeoutException {

		// components of new PSFT
		Collection<PSFTInputMove<A, B, C>> transitions = new ArrayList<>();
		Integer initialState = 0;
		Collection<Integer> finalStates = new HashSet<Integer>();

		HashSet<Integer> initStates = new HashSet<Integer>();
		initStates.add(aut.initialState);
		Collection<Integer> reachableFromInit = aut.getReachableStatesFrom(initStates);

		// Computes states that reachable from initial state and can reach a
		// final state
		Collection<Integer> aliveStates = new HashSet<>(reachableFromInit);
		for (Integer state : aliveStates)
			for (PSFTInputMove<A, B, C> t : aut.getInputMovesFrom(state))
				if (aliveStates.contains(t.to))
					transitions.add(t);

		initialState = aut.initialState;

		for (Integer state : aut.finalStates)
			if (aliveStates.contains(state))
				finalStates.add(state);

		return MkPSFT(transitions, initialState, finalStates, ba, false);
	}

	// Computes states that reachable from states
	private Collection<Integer> getReachableStatesFrom(Collection<Integer> states) {
		HashSet<Integer> result = new HashSet<Integer>();
		for (Integer state : states)
			visitForward(state, result);
		return result;
	}

	// Computes states that can reach states
	private Collection<Integer> getReachingStates(Collection<Integer> states) {
		HashSet<Integer> result = new HashSet<Integer>();
		for (Integer state : states)
			visitBackward(state, result);
		return result;
	}

	// DFS accumulates in reached
	private void visitForward(Integer state, HashSet<Integer> reached) {
		if (!reached.contains(state)) {
			reached.add(state);
			for (PSFTInputMove<PredicateT, DomainT, OutDomainT> t : this.getInputMovesFrom(state)) {
				Integer nextState = t.to;
				visitForward(nextState, reached);
			}
		}
	}

	// backward DFS accumulates in reached
	private void visitBackward(Integer state, HashSet<Integer> reached) {
		if (!reached.contains(state)) {
			reached.add(state);
			for (PSFTInputMove<PredicateT, DomainT, OutDomainT> t : this.getInputMovesTo(state)) {
				Integer predState = t.from;
				visitBackward(predState, reached);
			}
		}
	}

	// ------------------------------------------------------
	// Properties accessing methods
	// ------------------------------------------------------



	/**
	 * Returns the set of transitions starting set of states
	 */
	public Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> getTransitionsFrom(Collection<Integer> stateSet) {
		Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> transitions = new LinkedList<>();
		for (Integer state : stateSet)
			transitions.addAll(getInputMovesFrom(state));
		return transitions;
	}

	/**
	 * Returns the set of transitions to a set of states
	 */
	public Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> getTransitionsTo(Collection<Integer> stateSet) {
		Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> transitions = new LinkedList<>();
		for (Integer state : stateSet)
			transitions.addAll(getInputMovesTo(state));
		return transitions;
	}






	/**
	 * Returns the set of transitions to state <code>s</code>
	 */
	public Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> getInputMovesTo(Integer state) {
		Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> trset = inputMovesTo.get(state);
		if (trset == null) {
			trset = new HashSet<>();
			inputMovesTo.put(state, trset);
			return trset;
		}
		return trset;
	}

	/**
	 * Returns the set of transitions starting set of states
	 */
	public Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> getInputMovesTo(Collection<Integer> stateSet) {
		Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> transitions = new LinkedList<>();
		for (Integer state : stateSet)
			transitions.addAll(getInputMovesTo(state));
		return transitions;
	}

	/**
	 * Returns the set of transitions to state <code>s</code>
	 */
	public Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> getInputMovesFrom(Integer state) {
		Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> trset = inputMovesFrom.get(state);
		if (trset == null) {
			trset = new HashSet<>();
			inputMovesFrom.put(state, trset);
			return trset;
		}
		return trset;
	}

	/**
	 * Returns the set of transitions starting set of states
	 */
	public Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> getInputMovesFrom(Collection<Integer> stateSet) {
		if (stateSet.size() == 1) {
			return getInputMovesFrom(stateSet.iterator().next());
		}
		Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> transitions = new LinkedList<>();
		for (Integer state : stateSet)
			transitions.addAll(getInputMovesFrom(state));
		return transitions;
	}

	/**
	 * Returns the set of transitions starting set of states
	 */
	public Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> getTransitions() {
		Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> transitions = new LinkedList<>();
		for (Integer state : states)
			transitions.addAll(getInputMovesFrom(state));
		return transitions;
	}



	// ----------------------------------------------------
	// Overridden methods
	// ----------------------------------------------------

	public Collection<Move<PredicateT, DomainT>> getMovesFrom(Integer state) {
		Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> transitionsFrom = getInputMovesFrom(state);
		Collection<Move<PredicateT, DomainT>> transitions = new ArrayList<>(transitionsFrom);
		return transitions;
	}

	public Collection<Move<PredicateT, DomainT>> getMovesTo(Integer state) {
		Collection<PSFTInputMove<PredicateT, DomainT, OutDomainT>> transitionsTo = getInputMovesTo(state);
		Collection<Move<PredicateT, DomainT>> transitions = new ArrayList<>(transitionsTo);
		return transitions;
	}

	public Integer getInitialState() {
		return initialState;
	}

	public Collection<Integer> getFinalStates() {
		return finalStates;
	}

	public Collection<Integer> getNonFinalStates() {
		HashSet<Integer> nonFin = new HashSet<Integer>(states);
		nonFin.removeAll(finalStates);
		return nonFin;
	}

	public Collection<Integer> getStates() {
		return states;
	}

	public HashMap<Integer, List<DomainT>> getWitnesses(BooleanAlgebra<PredicateT, DomainT> ba) {
		try {
			HashSet<Integer> wip = new HashSet<>();
			HashMap<Integer, List<DomainT>> witnesses = new HashMap<>(stateCount());

			wip.add(initialState);
			witnesses.put(initialState, new ArrayList<>());

			while (wip.size()>0) {
				Integer next = wip.iterator().next();
				wip.remove(next);
				List<DomainT> witness = witnesses.get(next);
				for(PSFTInputMove<PredicateT, DomainT, OutDomainT> move : getInputMovesFrom(next)) {
					if (witnesses.containsKey(move.to)) {
						continue;
					} else {
						List<DomainT> newWit = new ArrayList<>(witness.size()+1);
						newWit.addAll(witness);
						newWit.add(ba.generateWitness(move.guard));
						witnesses.put(move.to, newWit);
						wip.add(move.to);
					}
				}
			}

			return witnesses;
		} catch (TimeoutException te) {
			throw new RuntimeException(te);
		}
	}

	@Override
	public Object clone() {
		PSFT<PredicateT, DomainT, OutDomainT> cl = new PSFT<>(new HashSet<Integer>(finalStates),
				new HashSet<>(states),
				new HashMap<>(inputMovesFrom),
				new HashMap<>(inputMovesTo),
				transitionCount,
				maxStateId);
		cl.isTotal = isTotal;

		cl.initialState = initialState;

		return cl;
	}

	public PSFT<PredicateT, DomainT, OutDomainT> cloneReadonly() {
		PSFT<PredicateT, DomainT, OutDomainT> cl = new PSFT<>(new HashSet<>(finalStates),
				Collections.unmodifiableCollection(new HashSet<>(states)),
				Collections.unmodifiableMap(new HashMap<>(inputMovesFrom)),
				Collections.unmodifiableMap(new HashMap<>(inputMovesTo)),
				transitionCount,
				maxStateId);
		cl.isTotal = isTotal;

		cl.initialState = initialState;

		return cl;
	}

}
