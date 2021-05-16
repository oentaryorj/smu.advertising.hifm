package larc.recommender.loss;

public abstract class Loss {
	public abstract double get(double pred, double target);
	public abstract double gradient(double pred, double target);
	public abstract double pairGradient(double pred_a, double pred_b, double target_a, double target_b);
	public abstract double transform(double x);
}
