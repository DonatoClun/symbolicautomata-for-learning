package utils;

import java.util.Arrays;

public class VertexColoring {

	public static int vertexColoring(boolean[][] conflictsMap, int[] result) {
		for (int n = 1; n < conflictsMap.length; n++) {
			boolean validColoringFound = kVertexColoring(conflictsMap, n, result);
			if (validColoringFound) {
				return n;
			}
		}
		throw new RuntimeException("cannot find a valid vertex coloring");
	}

	public static boolean kVertexColoring(boolean[][] conflictsMap, int k, int[] result) {
		Arrays.fill(result, 0);
		boolean allAttempted = false;
		while (!isValidColoring(conflictsMap, result)) {
			if (!nextColoring(result, k)) {
				allAttempted = true;
				break;
			}
		}
		return !allAttempted;
	}

	public static boolean nextColoring(int[] colors, int k) {
		colors[0]++;
		for (int n = 0; n < colors.length; n++) {
			if (colors[n] == k) {
				colors[n]=0;
				if (n+1 == colors.length) {
					return false;
				}
				colors[n+1]++;
			}
		}
		return true;
	}


	public static boolean isValidColoring(boolean[][] conflictsMap, int[] colors) {
		for (int a = 0; a < conflictsMap.length; a++) {
			for (int b = a + 1; b < conflictsMap.length; b++) {
				if (conflictsMap[a][b] && colors[a]==colors[b]) {
					return false;
				}
			}
		}
		return true;
	}
}
