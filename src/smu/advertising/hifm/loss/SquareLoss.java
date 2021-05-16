package larc.recommender.loss;

public class SquareLoss extends Loss {
	private final double min;
	private final double max;
	
	public SquareLoss(double min, double max) {
		this.min = min;
		this.max = max;
	}
	@Override
	public double get(double pred, double target) {
		double p = transform(pred);
		return /*0.5 **/ (p - target) * (p - target);
	}
	@Override
	public double gradient(double pred, double target) {
		return transform(pred) - target;
	}
	@Override
	public String toString() {
		return "square";
	}
	@Override
	public double transform(double x) {
		x = (x > max)? max : x;	// truncate prediction value
		x = (x < min)? min : x;
		return x;
	}
	@Override
	public double pairGradient(double pred_a, double pred_b, double target_a, double target_b) {
		return (transform(pred_a) - transform(pred_b)) - (target_a - target_b);
	}
}
