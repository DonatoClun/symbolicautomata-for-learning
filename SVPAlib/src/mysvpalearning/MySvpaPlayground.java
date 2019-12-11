package mysvpalearning;

import automata.sfa.SFA;
import automata.sfa.SFAInputMove;
import automata.sfa.SFAMove;
import automata.svpa.Internal;
import automata.svpa.Return;
import automata.svpa.SVPA;
import automata.svpa.SVPAMove;
import automata.svpa.Call;
import automata.svpa.TaggedSymbol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;

public class MySvpaPlayground {

	public static void test() throws Exception {
		UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();

		List<SFAMove<CharPred, Character>> transitions = new LinkedList<>();

		transitions.add(new SFAInputMove<>(0,1, new CharPred('a')));
		transitions.add(new SFAInputMove<>(0,2, new CharPred('(')));

		transitions.add(new SFAInputMove<>(1,0, new CharPred('+')));

		transitions.add(new SFAInputMove<>(2,4, new CharPred('[')));
		transitions.add(new SFAInputMove<>(2,3, new CharPred('a')));

		transitions.add(new SFAInputMove<>(3,2, new CharPred('+')));
		transitions.add(new SFAInputMove<>(3,1, new CharPred(')')));
		transitions.add(new SFAInputMove<>(3,7, new CharPred(')')));

		transitions.add(new SFAInputMove<>(4,5, new CharPred('a')));
		transitions.add(new SFAInputMove<>(4,6, new CharPred('{')));

		transitions.add(new SFAInputMove<>(5,4, new CharPred('+')));
		transitions.add(new SFAInputMove<>(5,3, new CharPred(']')));

		transitions.add(new SFAInputMove<>(6,7, new CharPred('a')));
		transitions.add(new SFAInputMove<>(6,2, new CharPred('(')));

		transitions.add(new SFAInputMove<>(7,6, new CharPred('+')));
		transitions.add(new SFAInputMove<>(7,5, new CharPred('}')));

		SFA<CharPred, Character> res = SFA.MkSFA(transitions, 0, Collections.singleton(1), ba);

		List<Character> witness = res.getWitness(ba);

		res = res.determinize(ba);

		res.createDotFile("bla", "/home/denny/Documents/research/grammarlearning/");
	}

	public static void main(String[] args) throws Exception {
		test();

//		UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
//		for (int n = 1; n < 5000; n++) {
//			try {
//				SVPA<CharPred, Character> svpa = getRandomSvpa(20, 2, 60, ba, n);
//				svpa.createDotFile("svpa", "/home/denny/Documents/research/grammarlearning/");
//
//				LinkedList<TaggedSymbol<Character>> witness = svpa.getWitness(ba);
//				if (witness.size() < 5)
//					continue;
//				System.out.println(witness.toString());
//			} catch (Exception e) {
//
//			}
//		}
	}

	public static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	public static SVPA<CharPred, Character> getRandomSvpa(int genStates, int genFinalStates, int genTransitions, UnaryCharIntervalSolver ba, int seed)
			throws Exception {
		HashMap<Integer, List<SVPAMove<CharPred, Character>>> transitionMap = new HashMap<>();
		Set<Integer> initialStates = Collections.singleton(0);
		List<Integer> finalStates = new LinkedList<>();

		Random rnd = new Random(seed);

		while (--genTransitions >= 0) {
			int from = rnd.nextInt(genStates);
			List<SVPAMove<CharPred, Character>> otherTransitionsFromThisState;
			if (transitionMap.containsKey(from)) {
				otherTransitionsFromThisState = transitionMap.get(from);
			} else {
				otherTransitionsFromThisState = new LinkedList<>();
				transitionMap.put(from, otherTransitionsFromThisState);
			}


			boolean toFound;
			int to;
			int maxcnt = genStates;
			do {
				toFound = true;
				to = rnd.nextInt(genStates);
				for (SVPAMove<CharPred, Character> otherTr : otherTransitionsFromThisState) {
					if (otherTr.to == to) {
						toFound = false;
					}
				}
			} while (!toFound && --maxcnt > 0);

			SVPAMove<CharPred, Character> generatedTransition;
			CharPred guard = new CharPred(ALPHABET.charAt(rnd.nextInt(ALPHABET.length())));

			int stackState = rnd.nextInt(2);
			switch (rnd.nextInt(5)) {
				case 0: //CALL
				case 1:
				case 2:
					generatedTransition = new Call<>(from, to, stackState, guard);
					break;
				case 3: //INTERNAL
					//generatedTransition = new Internal<>(from, to, guard);
					//break;
				case 4: //RETURN
				case 5:
				case 6:
					generatedTransition = new Return<>(from, to, stackState, guard);
					break;
				default:
					throw new Exception();
			}

			List<SVPAMove<CharPred, Character>> l;
			if (!transitionMap.containsKey(from)) {
				l = new LinkedList<>();
				transitionMap.put(from, l);
			} else {
				l = transitionMap.get(from);
			}

			l.add(generatedTransition);

		}

		while (--genFinalStates >= 0) {
			int f = 1+rnd.nextInt(genStates-1);
			finalStates.add(f);
		}

		List<SVPAMove<CharPred, Character>> transitions = new ArrayList<>();
		transitionMap.forEach((k,v)-> transitions.addAll(v));
		SVPA<CharPred, Character> result = SVPA.MkSVPA(transitions, initialStates, finalStates, ba);
		return result;
	}

}
