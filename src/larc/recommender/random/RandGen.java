package larc.recommender.random;

import java.util.Random;

public class RandGen {
	private static final Random rand = new Random(0);
	
	public static double erf(double x) {
		double t = (x >= 0)? (1.0 / (1.0 + 0.3275911 * x)) : (1.0 / (1.0 - 0.3275911 * x));
		double result = 1.0 - (t * (0.254829592 + t * (-0.284496736 + t * (1.421413741 + t * (-1.453152027 + t * 1.061405429)))))*Math.exp(-x*x);
		return (x >= 0)? result : -result;
	}
	public static double cdfGaussian(double x, double mean, double stdev) {
		return 0.5 + 0.5 * erf(0.707106781 * (x-mean) / stdev);
	}
	public static double cdfGaussian(double x) {
		return 0.5 + 0.5 * erf(0.707106781 * x );
	}
	public static double leftTGaussian(double left) {
		// draw a trunctated normal: acceptance region are values larger than <left>
		if (left <= 0.0) { // acceptance probability > 0.5
			return leftTGaussianNaive(left);
		} 
		// Robert: Simulation of truncated normal variables
		double alpha_star = 0.5*(left + Math.sqrt(left*left + 4.0));

		// draw from translated Math.exponential distr:
		// f(alpha,left) = alpha * Math.exp(-alpha*(z-left)) * I(z>=left)
		double z,d,u;
		do {
			z = exp() / alpha_star + left;
			d = z-alpha_star;
			d = Math.exp(-(d*d)/2);
			u = uniform();
			if (u < d) {
				return z;
			}
		} while (true);
	}
	public static double leftTGaussianNaive(double left) {
		// draw a trunctated normal: acceptance region are values larger than <left>
		double result;
		do {
			result = gaussian();
		} while (result < left);
		return result;
	}
	public static double leftTGaussian(double left, double mean, double stdev) {
		return mean + stdev * leftTGaussian((left-mean)/stdev); 
	}
	public static double rightTGaussian(double right) {
		return -leftTGaussian(-right);
	}
	public static double rightTGaussian(double right, double mean, double stdev) {
		return mean + stdev * rightTGaussian((right-mean)/stdev); 
	}
	public static double gamma(double alpha) {
		if (alpha <= 0) {
			throw new IllegalArgumentException("Alpha must be positive");
		}
		if (alpha < 1.0) {
			double u;
			do {
				u = uniform();
			} while (u == 0.0);
			return gamma(alpha + 1.0) * Math.pow(u, 1.0 / alpha);
		} 
		// Marsaglia and Tsang: A Simple Method for Generating Gamma Variables
		double d,c,x,v,u;
		d = alpha - 1.0/3.0;
		c = 1.0 / Math.sqrt(9.0 * d);
		do {
			do {
				x = gaussian();
				v = 1.0 + c*x;
			} while (v <= 0.0);
			v = v * v * v;
			u = uniform();
		} while ((u >= (1.0 - 0.0331 * (x*x) * (x*x))) && (Math.log(u) >= (0.5 * x * x + d * (1.0 - v + Math.log(v)))));
		return d*v;
	}
	public static double gamma(double alpha, double beta) {
		return gamma(alpha) / beta;
	}
	public static double gaussian() {
		// Joseph L. Leva: A fast normal Random number generator
		double u,v, x, y, Q;
		do {
			do {
				u = uniform();
			} while (u == 0.0); 
			v = 1.7156 * (uniform() - 0.5);
			x = u - 0.449871;
			y = Math.abs(v) + 0.386595;
			Q = x*x + y*(0.19600*y-0.25472*x);
			if (Q < 0.27597) { 
				break; 
			}
		} while ((Q > 0.27846) || ((v*v) > (-4.0*u*u*Math.log(u)))); 
		return v / u;
	}
	public static double gaussian(double mean, double stdev) {
		if (stdev == 0.0 || Double.isNaN(stdev)) {
			return mean;
		} 
		return mean + stdev*gaussian();
	}
	public static double uniform() {
		return rand.nextDouble();
	}
	public static double exp() {
		return -Math.log(1-uniform());
	}
	public static boolean bernoulli(double p) {
		return (uniform() < p);
	}
}