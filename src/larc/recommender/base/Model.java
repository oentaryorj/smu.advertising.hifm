package larc.recommender.base;

import larc.recommender.data.Feature;
import larc.recommender.data.Instance;
import larc.recommender.random.RandGen;

public class Model {
	public double w0;        				// global bias
	public final double[] W;				// bias
	public final double[][] V;				// interaction weight
	public final int numFactors;
		
	public Model(int numCols, int numFactors, double sigma) {
		this.numFactors = numFactors;
		this.W = new double[numCols];
		this.V = new double[numFactors][numCols];
		init(sigma);
	}
	public Model init(double sigma) {
		this.w0 = 0.0;
		for (int j = 0; j < W.length; j++) {
			this.W[j] = 0.0;								// initialize W to zero
			if (sigma > 0.0) {
				for (int k = 0; k < numFactors; k++) {		
					V[k][j] = RandGen.gaussian(0.0, sigma); // initialize V such that V ~ N(0, sigma)
				}
			} else {
				for (int k = 0; k < numFactors; k++) {		
					V[k][j] = 0.0; 							// initialize V such that V ~ N(0, sigma)
				}
			}
		}
		return this;
	}
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("Model\n\n");
		str.append("w0 = ").append(this.w0).append("\n\n");
		str.append("W =\n");
		for (int j = 0; j < this.W.length; j++) {
			str.append(this.W[j]).append(" ");
		}
		str.append("\n\n");
		str.append("V =\n");
		for (int k = 0; k < this.numFactors; k++) {
			for (int j = 0; j < this.V[k].length; j++) {
				str.append(this.V[k][j]).append(" ");
			}
			str.append("\n");
		}
		return str.toString().trim();
	}
	public double output(Instance x, double[] sums) {
		if (sums.length != numFactors) {
			throw new IllegalArgumentException("Invalid sum length");
		}
		double sum = 0.0, sumSqr = 0.0;
		double pred = w0;
		for (Feature f : x) {
			pred += W[f.index] * f.value;	// j = e.getKey(); x_j = e.value;
		}
		for (int k = 0; k < numFactors; k++) {
			sum = sumSqr = 0.0;
			for (Feature f : x) {
				double prod = V[k][f.index] * f.value;	// j = e.getKey(); x_j = e.value;
				sum += prod;
				sumSqr += prod * prod;
			}
			pred += 0.5 * (sum * sum - sumSqr);
			sums[k] = sum;		// cache the sum values
		}
		return pred; 
	}
}
