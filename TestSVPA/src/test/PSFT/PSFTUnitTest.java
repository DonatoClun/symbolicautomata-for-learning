package test.PSFT;

import static org.junit.Assert.*;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;
import transducers.psft.PSFT;
import transducers.psft.PSFTInputMove;

public class PSFTUnitTest {

	@Test
	public void testSingleState() throws TimeoutException {
		PSFT<CharPred, Character, Character> ft;

		UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();

		ArrayList<PSFTInputMove<CharPred, Character, Character>> transitions = new ArrayList<>();
		for (int n = 0; n < 10; n++) {
			transitions.add(new PSFTInputMove<>(0, 0,
					new CharPred(String.valueOf(n).charAt(0)), String.valueOf((n+1)%10).charAt(0)));
		}

		transitions.add(new PSFTInputMove<>(0, 0, ba.MkNot(new CharPred('0', '9')), 'X'));

		ft = PSFT.MkPSFT(transitions, 0, Collections.EMPTY_LIST, ba);

		checkTranslation(ft, ba);
		ft = ft.mkTotal(ba);
		checkTranslation(ft, ba);
	}

	private void checkTranslation(PSFT<CharPred, Character, Character> ft, UnaryCharIntervalSolver ba)
			throws TimeoutException {
		List<Character> tra;
		tra = ft.translate(ba, '0','1','2','9','A');
		assertEquals(5, tra.size());
		assertEquals(new Character('1'), tra.get(0));
		assertEquals(new Character('2'), tra.get(1));
		assertEquals(new Character('3'), tra.get(2));
		assertEquals(new Character('0'), tra.get(3));
		assertEquals(new Character('X'), tra.get(4));
	}

	@Test
	public void testDiv() throws TimeoutException {
		PSFT<CharPred, Character, Character> ft;

		UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();

		ArrayList<PSFTInputMove<CharPred, Character, Character>> transitions = new ArrayList<>();

		transitions.add(new PSFTInputMove<>(0, 0, new CharPred('0'), '0'));
		transitions.add(new PSFTInputMove<>(0, 1, new CharPred('1'), '0'));
		transitions.add(new PSFTInputMove<>(1, 0, new CharPred('1'), '1'));
		transitions.add(new PSFTInputMove<>(1, 2, new CharPred('0'), '0'));
		transitions.add(new PSFTInputMove<>(2, 2, new CharPred('1'), '1'));
		transitions.add(new PSFTInputMove<>(2, 1, new CharPred('0'), '1'));

		ft = PSFT.MkPSFT(transitions, 0, Collections.EMPTY_LIST, ba);

		ft = ft.mkTotal(ba);

		for (int n = 0; n < 300; n++) {
			assertEquals(n / 3, binStrToInt(ft.translate(intToBinStr(n), ba)));
		}

	}

	public static List<Character> intToBinStr(int n) {
		String s = Integer.toBinaryString(n);
		return new AbstractList<Character>() {
			@Override
			public Character get(int index) {
				return s.charAt(index);
			}

			@Override
			public int size() {
				return s.length();
			}
		};
	}

	public static int binStrToInt(List<Character> str) {
		int result = 0;
		int val = 1;
		for (int pos = str.size() - 1; pos >= 0; pos--) {
			Character character = str.get(pos);
			if (character.equals('1')) {
				result = result + val;
			}
			val = val * 2;
		}
		return result;
	}

}
