package larc.recommender.base;

public class Config {
	public int nfactors = 5;			// number of latent factors
	public boolean projL2 = false;		// whether to do projection to L2 norm balls
	public double min = 0.0;			// minimum target value
	public double max = 1.0;			// maximum target value
	public double reg0 = 0.0;			// regularization term for w0 update
	public double regW = 0.01;			// regularization term for W update
	public double regV = 0.01;		    // regularization term for V update
	public double eta = 0.01;			// learning rate for SGD
	public double sigma = 0.01;			// initial noise parameter for V
	public double maxIter = 100;		// maximum optimization iteration
	//public double alpha = 1.0;    		// trade-off between regression and ranking loss functions
	
	public final double alpha0 = 1.0;	// MCMC hyper-parameters
	public final double beta0 = 1.0;
	public final double alphaReg = 1.0;
	public final double betaReg = 1.0;
	public final double gamma0 = 1.0;
	public final double miu0 = 0.0;
	public boolean stochastic = true;
	
	public Config() {
		super();
	}
	public Config setBounds(double min, double max) {
		if (min > max) {
			throw new IllegalArgumentException("Min bound should be lesser than max bound");
		}
		this.min = min;
		this.max = max;
		return this;
	}
	@Override
	public String toString() {
		return "Config [nfactors=" + nfactors + ", min=" + min + ", max=" + max
				+ ", reg0=" + reg0 + ", regW=" + regW + ", regV=" + regV
				+ ", eta=" + eta + ", sigma=" + sigma + ", maxIter=" + maxIter
				+ ", alpha=" + alpha + ", alpha0=" + alpha0 + ", beta0=" + beta0 
				+ ", alphaReg=" + alphaReg + ", betaReg=" + betaReg
				+ ", gamma0=" + gamma0 + ", miu0=" + miu0;
	}
}
