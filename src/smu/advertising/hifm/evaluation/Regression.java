package larc.recommender.evaluation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Regression {
	private final List<Double> predictions;
	private final List<Double> targets;
	private final List<Double> weights;
	
	public Regression(int size) {
		predictions = new ArrayList<Double>(size);
		targets = new ArrayList<Double>(size);
		weights = new ArrayList<Double>(size);
	}
	public void add(double target, double pred, double weight) {
		targets.add(target);
		predictions.add(pred);
		weights.add(weight);
	}
	public void clear() {
		targets.clear();
		predictions.clear();
		weights.clear();
	}
	public int size() {
		return targets.size();
	}
	public double MSE() {
		double sumSqr = 0.0, sumWeight = 0.0;
		for (int i = 0; i < targets.size(); i++) {
			double diff = targets.get(i) - predictions.get(i);
			sumSqr += diff * diff * weights.get(i);
			sumWeight += weights.get(i);
		}
		return sumSqr / sumWeight;
	}
	public double MAE() {
		double sum = 0.0, sumWeight = 0.0;
		for (int i = 0; i < targets.size(); i++) {
			sum += Math.abs(targets.get(i) - predictions.get(i)) * weights.get(i);
			sumWeight += weights.get(i);
		}
		return sum / sumWeight;
	}
	public double logistic() {
		double epsilon = 1e-8;
		double sum = 0.0, sumWeight = 0.0;
		for (int i = 0; i < targets.size(); i++) {
			double t = targets.get(i);
			double p = predictions.get(i);
			if (t < 0.0 || t > 1.0) {
				System.out.println(" WARNING: Line " + (i+1) + " " + t);
				throw new IllegalArgumentException("Target values are invalid");
			} else if (p < 0.0 || p > 1.0) {
				throw new IllegalArgumentException("Prediction values are invalid");
			}
			p = (p < epsilon)? epsilon : p;
			p = (p > 1.0 - epsilon)? (1.0 - epsilon) : p;
			sum -= (t * Math.log(p) + (1.0 - t) * Math.log(1.0 - p)) * weights.get(i);
			sumWeight += weights.get(i);
		}
		return sum / sumWeight;
	}
	public double correl() {
		double sum_sq_x = 0.0, sum_sq_y = 0.0, sum_coproduct = 0.0;
		double mean_x = predictions.get(0);
		double mean_y = targets.get(0);
		for(int i = 2; i <= targets.size(); i++){
			double sweep = (double) (i-1) / (double) i;
			double delta_x = predictions.get(i-1) - mean_x;
			double delta_y = targets.get(i-1) - mean_y;
			sum_sq_x += delta_x * delta_x * sweep;
			sum_sq_y += delta_y * delta_y * sweep;
			sum_coproduct += delta_x * delta_y * sweep;
			mean_x += delta_x / i;
			mean_y += delta_y / i;
		}
		double pop_sd_x = Math.sqrt(sum_sq_x);
		double pop_sd_y = Math.sqrt(sum_sq_y);
		double cov_x_y = sum_coproduct;
		return (pop_sd_x != 0.0 && pop_sd_y != 0.0)? (cov_x_y / (pop_sd_x * pop_sd_y)) : 0.0;
	}
	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("0.########");
		StringBuilder str = new StringBuilder();
		str.append("RMSE=").append(df.format(Math.sqrt(MSE())));
		str.append(" MAE=").append(df.format(MAE()));
		str.append(" LL=").append(df.format(logistic()));
		str.append(" R=").append(df.format(correl()));
		return str.toString().trim();
	}
}
