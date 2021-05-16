package larc.recommender.learn;

import larc.recommender.base.Config;
import larc.recommender.data.Data;
import larc.recommender.data.Feature;
import larc.recommender.data.Instance;
import larc.recommender.loss.Loss;
import larc.recommender.random.RandGen;
import larc.recommender.regularization.Regularization;

public class MCMC extends CD {
	protected double alpha = 1.0;
	protected double lambdaW = 0.0;
	protected final double[] lambdaV;
	protected double miuW = 0.0;
	protected final double[] miuV;
	
	public MCMC(int numCols, Config params, Loss loss, Regularization reg) {
		super(numCols, params, loss, reg);
		this.lambdaV = new double[params.nfactors];
		this.miuV = new double[params.nfactors];
	}
	@Override
	public double[] learn(Data train, Data validation) {
		String title = "Data: " + train.getName() + "\n" + params;
		trace = title + "\n";
		System.out.println(title);
		fm.init(params.sigma);		// initialize the weight parameters
		eCache = new double[train.getNumRows()];
		qCache = new double[train.getNumRows()];
		double[] sumPreds = new double[validation.getNumRows()];
		double[] avgPreds = new double[validation.getNumRows()];
		for (int i = 0; i < sumPreds.length; i++) {
			sumPreds[i] = 0.0;	// reset
		}
		long totalTime = 0;
		for (int n = 0; n < params.maxIter; n++) {
			long time = System.currentTimeMillis();
			update(train);
			time = System.currentTimeMillis() - time;
			totalTime += time;
			double[] preds = predict(validation);
			for (int i = 0; i < preds.length; i++) {
				sumPreds[i] += preds[i];
				avgPreds[i] = sumPreds[i] / (double) (n + 1);
			}
			StringBuilder log = new StringBuilder();
			double[] predTrain = predict(train);
			log.append(" Iteration ").append(n+1).append(":");
			log.append(" Train_loss=").append(df.format(loss(train, predTrain, false)));
			log.append(" Train_loss_weight=").append(df.format(loss(train, predTrain, true)));
			log.append(" Validation_loss=").append(df.format(loss(validation, avgPreds, false)));
			log.append(" Validation_loss_weight=").append(df.format(loss(validation, avgPreds, true)));
			log.append(" Time=").append(time).append("ms Total_time=").append(totalTime).append("ms");
			System.out.println(log.toString());
			trace += log.toString() + "\n";
		}
		return avgPreds;
	}
	@Override
	protected void update(Data data) {
		int numRows = data.getNumRows();		
		if (eCache.length != numRows) {
			throw new IllegalArgumentException("Inconsistent size!");
		}
		for (int i = 0; i < data.getNumRows(); i++) {	// pre-compute cache e
			Instance x = data.row(i);
			eCache[i] = x.target - predict(x, sums);
		}
		updateHyperParams(data);	// update the hyperparameters
		updateParams(data);			// update the model parameters
	}
	public double predict(Instance x, double[] sums) {
		return loss.transform(fm.output(x, sums));
	}
	protected void updateHyperParams(Data data) {
		int numRows = eCache.length;
		int numCols = fm.W.length;
		// 1a) Sample alpha
		double a = params.alpha0 + (double) numRows;
		double b = params.beta0;
		for (int i = 0; i < numRows; i++) {
			b += eCache[i] * eCache[i]; 	// Note: The main difference with unweighted MCMC
		}
		alpha = RandGen.gamma(0.5 * a, 0.5 * b);
		//System.out.println("Draw alpha: Alpha=" + alpha + ", a=" + a + ", b=" + b);
		// 1b) Sample lamdaW and miuW
		a = params.alphaReg + numCols + 1;
		b = params.betaReg + params.gamma0 * (miuW - params.miu0) * (miuW - params.miu0);
		double u = params.gamma0 * params.miu0;
		double s = numCols + params.gamma0;
		for (int j = 0; j < numCols; j++) {
			b += (fm.W[j] - miuW) * (fm.W[j] - miuW);
			u += fm.W[j];
		}
		lambdaW = RandGen.gamma(0.5 * a, 0.5 * b);
		miuW = (lambdaW != 0.0)? RandGen.gaussian(u / s, Math.sqrt(1.0 / (s * lambdaW))) : miuW;	
		//System.out.println("Draw miuW, regW: lambdaW=" + lambdaW + " miuW=" + miuW + ", a=" + a + ", b=" + b + ", u=" + u + ", s=" + s);	
		// 1c) Sample lamdaV and miuV
		for (int k = 0; k < fm.numFactors; k++) {
			a = params.alphaReg + numCols + 1;
			b = params.betaReg + params.gamma0 * (miuV[k] - params.miu0) * (miuV[k] - params.miu0);
			u = params.gamma0 * params.miu0;
			s = numCols + params.gamma0;
			for (int j = 0; j < numCols; j++) {
				b += (fm.V[k][j] - miuV[k]) * (fm.V[k][j] - miuV[k]);
				u += fm.V[k][j];
			}
			lambdaV[k] = RandGen.gamma(0.5 * a, 0.5 * b);
			miuV[k] = (lambdaV[k] != 0.0)? RandGen.gaussian(u / s, Math.sqrt(1.0 / (s * lambdaV[k]))) : miuV[k];	
			//System.out.println("Draw miuV, regV" + k + ", lambdaV=" + lambdaV[k] + " miuV=" + miuV[k] + ", a=" + a + ", b=" + b + ", u=" + u + ", s=" + s);
		}
	}
	protected void updateParams(Data data) {
		int numRows = eCache.length;
		if (numRows != data.getNumRows()) {
			throw new IllegalArgumentException("Inconsistent data size");
		}
		double sumErr = 0.0;
		double sumSqr = (double) numRows;
		for (int i = 0; i < numRows; i++) {
			sumErr += eCache[i]; 
		}
		if (sumErr == 0.0) {
			return;
		}
		double mean = 0.0, var = 0.0, newWeight = 0.0;
		double denom = alpha * sumSqr + params.reg0;
		if (denom != 0.0) {
			var = 1.0 / denom;
			mean = var * (alpha * (fm.w0 * sumSqr + sumErr) + params.miu0 * params.reg0);
			newWeight = RandGen.gaussian(mean, Math.sqrt(var));
			for (int i = 0; i < numRows; i++) {						// update e-cache
				eCache[i] += fm.w0 - newWeight; 
			}
			fm.w0 = newWeight;				// update W0
		} 
		int numCols = data.getNumCols();
		for (int j = 0; j < fm.W.length; j++) {
			Instance xt = (j < numCols)? data.col(j) : zeroInstance;
			sumErr = sumSqr;		// reset
			for (Feature f : xt) {
				double h = f.value;
				sumSqr += h * h;
				sumErr += h * eCache[f.index]; 
			}
			denom = alpha * sumSqr + lambdaW;
			if (denom != 0.0) {
				var = 1.0 / denom;
				mean = var * (alpha * (fm.W[j] * sumSqr + sumErr) + reg.CD(fm.W, j, lambdaW) + miuW * lambdaW);
				newWeight = RandGen.gaussian(mean, Math.sqrt(var));
				for (Feature f : xt) {				// update e-cache
					eCache[f.index] += (fm.W[j] - newWeight) * f.value; 
				}
				fm.W[j] = newWeight;		// update W_j
			} 
		}
		for (int k = 0; k < fm.V.length; k++) {
			for (int i = 0; i < numRows; i++) {					// initialize q-cache
				qCache[i] = 0.0;	// reset
				Instance x = data.row(i);
				for (Feature f : x) {
					qCache[i] += fm.V[k][f.index] * f.value;	// j = e.getKey(); x_j = e.value;
				}
			}
			for (int j = 0; j < fm.V[k].length; j++) {
				Instance xt = (j < numCols)? data.col(j) : zeroInstance;
				sumErr = sumSqr = 0.0;		// reset
				for (Feature f : xt) {
					double h = f.value * (qCache[f.index] - fm.V[k][j] * f.value); 
					sumSqr += h * h;
					sumErr += h * eCache[f.index];
				}
				denom = alpha * sumSqr + lambdaV[k];
				if (denom != 0.0) {
					var = 1.0 / denom;
					mean = var * (alpha * (fm.V[k][j] * sumSqr + sumErr) + reg.CD(fm.V[k], j, lambdaV[k]) + miuV[k] * lambdaV[k]);
					newWeight = RandGen.gaussian(mean, Math.sqrt(var));
					for (Feature f : xt) {							// update e-cache and q-cache
						double diff = f.value * (fm.V[k][j] - newWeight);
						eCache[f.index] += diff * (qCache[f.index] - fm.V[k][j] * f.value);
						qCache[f.index] -= diff; 
					}
					fm.V[k][j] = newWeight;		// update V_kj
				} 
			}
		}
		//System.out.println("reg0=" + lambda0 + " regW=" + lambdaW + " w0=" + fm.w0 + " W1=" + fm.W[0] + " V11=" + fm.V[0][0]);
	}
}
