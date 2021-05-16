package larc.recommender.regularization;

public class L2 extends Regularization {
	@Override
	public double SGD(double[] weights, int idx, double eta, double lambda) {
		return weights[idx] * (1.0 - eta * lambda);
	}
	@Override
	public double SGD(double weight, double eta, double lambda) {
		return weight * (1.0 - eta * lambda);
	}
	@Override
	public double CD(double[] weights, int idx, double lambda) {
		return 0.0;
	}
	@Override
	public double CD(double weight, double lambda) {
		return 0.0;
	}
	@Override
	public String toString() {
		return "L2";
	}
}
