package larc.recommender.regularization;

public abstract class Regularization {
	public abstract double SGD(double[] weights, int idx, double eta, double lambda);
	public abstract double SGD(double weight, double eta, double lambda);
	public abstract double CD(double[] weights, int idx, double lambda);
	public abstract double CD(double weight, double lambda);
}
