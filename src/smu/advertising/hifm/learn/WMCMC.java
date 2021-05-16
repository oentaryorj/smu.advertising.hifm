package larc.recommender.learn;

import larc.recommender.base.Config;
import larc.recommender.data.Data;
import larc.recommender.data.Feature;
import larc.recommender.data.Instance;
import larc.recommender.loss.Loss;
import larc.recommender.random.RandGen;
import larc.recommender.regularization.Regularization;

public class WMCMC extends MCMC {
	public WMCMC(int numCols, Config params, Loss loss, Regularization reg) {
		super(numCols, params, loss, reg);
	}
	@Override
	protected void updateHyperParams(Data data) {
		int numRows = eCache.length;
		int numCols = fm.W.length;
		// 1a) Sample alpha
		double a = params.alpha0;
		double b = params.beta0;
		for (int i = 0; i < numRows; i++) {
			double w = data.row(i).weight; //Math.log(data.row(i).weight);
			b += w * eCache[i] * eCache[i]; 	// Note: The main difference with unweighted MCMC
			a += w;
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
	@Override
	protected void updateParams(Data data) {
		int numRows = eCache.length;
		if (numRows != data.getNumRows()) {
			throw new IllegalArgumentException("Inconsistent data size");
		}
		double sumErr = 0.0, sumSqr = 0.0;
		for (int i = 0; i < numRows; i++) {
			double w = data.row(i).weight;
			sumErr += eCache[i] * w; 
			sumSqr += w;
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
				double w = data.row(f.index).weight; //Math.log(data.row(f.index).weight);
				double h = f.value;
				sumSqr += h * h * w;
				sumErr += h * eCache[f.index] * w; 
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
					double w = data.row(f.index).weight;
					double h = f.value * (qCache[f.index] - fm.V[k][j] * f.value); 
					sumSqr += h * h * w;
					sumErr += h * eCache[f.index] * w;
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
