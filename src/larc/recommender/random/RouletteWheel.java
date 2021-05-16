package larc.recommender.random;

import java.util.Arrays;
import java.util.Random;

public class RouletteWheel {
	private static final Random rand = new Random(0);
	private final double[] cumulatives;
	
	public RouletteWheel(double[] values) {
		cumulatives = new double[values.length];
		cumulatives[0] = values[0];
		for (int i = 1; i < values.length; i++) {
			cumulatives[i] = cumulatives[i - 1] + values[i];
		}
	}
	public int[] select(int count) {
		int[] selections = new int[count];
		for (int i = 0; i < count; i++) {
			selections[i] = select();
		}
		return selections;
	}
	public int select() {
		double randomFitness = rand.nextDouble() * cumulatives[cumulatives.length - 1];
		int index = Arrays.binarySearch(cumulatives, randomFitness);
		if (index < 0) {	// Convert negative insertion point to array index.
			index = Math.abs(index + 1);
		}
		return index;
	}
}
