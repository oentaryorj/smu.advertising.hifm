package larc.recommender.loss;

public class LogisticLoss extends Loss {
	public LogisticLoss() {
		super();
	}
	@Override
	public double get(double pred, double target) {
		if (target < 0.0 || target > 1.0) {
			throw new IllegalArgumentException("Target must be within interval [0, 1]");
		} else if (pred <= 0.0 || pred >= 1.0) {
			throw new IllegalArgumentException("Target must be within interval (0, 1)");
		}
		return -(target * Math.log(pred) + (1.0 - target) * Math.log(1.0 - pred));
	}
	@Override
	public double gradient(double pred, double target) {
		if (target < 0.0 || target > 1.0) {
			throw new IllegalArgumentException("Target must be within interval [0, 1]");
		}
		return (transform(pred) - target);
	}
	@Override
	public String toString() {
		return "logistic";
	}
	@Override
	public double transform(double x) {
		return 1.0 / (1.0 + Math.exp(-x));
	}
	@Override
	public double pairGradient(double pred_a, double pred_b, double target_a, double target_b) {
		if (target_a < 0.0 || target_a > 1.0 || target_b < 0.0 || target_b > 1.0) {
			throw new IllegalArgumentException("Target must be within interval [0, 1]");
		}
		double target = (target_a > target_b)? 1.0 : (target_a < target_b)? 0.0 : 0.5;
		return transform(pred_a - pred_b) - target; 
		//return transform(pred_a - pred_b) - 0.5 * (1.0 + target_a - target_b);
	}
}
