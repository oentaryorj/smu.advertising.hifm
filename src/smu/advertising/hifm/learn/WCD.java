package larc.recommender.learn;

import java.util.ArrayList;
import java.util.Collections;

import larc.recommender.base.Config;
import larc.recommender.data.Data;
import larc.recommender.data.Feature;
import larc.recommender.data.Instance;
import larc.recommender.loss.Loss;
import larc.recommender.regularization.Regularization;

public class WCD extends CD {
	public WCD(int numCols, Config params, Loss loss, Regularization reg) {
		super(numCols, params, loss, reg);
	}
	protected void update(Data data) {
		int numRows = data.getNumRows();		
		if (eCache.length != numRows) {
			throw new IllegalArgumentException("Inconsistent size!");
		}
		double sumErr = 0.0, sumSqr = 0.0;
		for (int i = 0; i < numRows; i++) {	// pre-compute cache e and q
			Instance x = data.row(i);
			eCache[i] = x.target - predict(x, sums);
			sumErr += x.weight * eCache[i];
			sumSqr += x.weight;
		}
		if (sumErr == 0.0) {
			return;
		}
		double denom = sumSqr + params.reg0 * numRows;
		if (denom != 0.0) {
			double newW0 = (fm.w0 * sumSqr + sumErr) / denom;
			for (int i = 0; i < numRows; i++) {						// update e-cache
				eCache[i] += fm.w0 - newW0; 
			}
			fm.w0 = newW0;				// update W0
		}
		int numCols = data.getNumCols();
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int p = 0; p < fm.W.length; p++) {
			list.add(p);
		}
		if (params.stochastic) {
			Collections.shuffle(list);
		}
		for (Integer j : list) {
			Instance xt = (j < numCols)? data.col(j) : zeroInstance;
			sumErr = sumSqr = 0.0;		// reset
			for (Feature f : xt) {
				double w = data.row(f.index).weight;
				double h = f.value;
				sumSqr += h * h * w;
				sumErr += h * eCache[f.index] * w; 
			}
			denom = sumSqr + params.regW * numRows;
			if (denom != 0.0) {
				double newW = (fm.W[j] * sumSqr + sumErr + reg.CD(fm.W, j, params.regW)) / denom; 
				for (Feature f : xt) {								// update e-cache
					eCache[f.index] += (fm.W[j] - newW) * f.value; 
				}
				fm.W[j] = newW;		// update W_j
			}
		}
		for (int k = 0; k < fm.V.length; k++) {
			for (int i = 0; i < numRows; i++) {					// initialize q-cache
				qCache[i] = 0.0;	// reset
				for (Feature f : data.row(i)) {
					qCache[i] += fm.V[k][f.index] * f.value;	// j = e.getKey(); x_j = e.value;
				}
			}
			for (Integer j : list) {
				Instance xt = (j < numCols)? data.col(j) : zeroInstance;
				sumErr = sumSqr = 0.0;		// reset
				for (Feature f : xt) {
					double w = data.row(f.index).weight;
					double h = f.value * (qCache[f.index] - fm.V[k][j] * f.value); 
					sumSqr += h * h * w;
					sumErr += h * eCache[f.index] * w; 
				}
				denom = sumSqr + params.regV * numRows;
				if (denom != 0.0) {
					double newV = (fm.V[k][j] * sumSqr + sumErr + reg.CD(fm.V[k], j, params.regV)) / denom;
					for (Feature f : xt) {							// update e-cache and q-cache
						double diff = f.value * (fm.V[k][j] - newV);
						eCache[f.index] += diff * (qCache[f.index] - fm.V[k][j] * f.value);
						qCache[f.index] -= diff; 
					}
					fm.V[k][j] = newV;		// update V_kj
				}
			}
		}
	}
	@Override
	protected double loss(Data data, double[] preds) {
		if (data.getNumRows() != preds.length) {
			throw new IllegalArgumentException("Data size does not match prediction size");
		}
		double sumSqr = 0.0, sumWeight = 0.0;
		for (int i = 0; i < preds.length; i++) {
			Instance x = data.row(i);
			double diff = x.target - preds[i];
			sumSqr += diff * diff * x.weight;
			sumWeight += x.weight;
		}
		return Math.sqrt(sumSqr / sumWeight);
	}
}
