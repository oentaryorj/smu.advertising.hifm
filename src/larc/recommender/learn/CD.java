package larc.recommender.learn;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;

import larc.recommender.base.Config;
import larc.recommender.base.Model;
import larc.recommender.data.Data;
import larc.recommender.data.Feature;
import larc.recommender.data.Instance;
import larc.recommender.loss.Loss;
import larc.recommender.regularization.Regularization;

public class CD extends Learn {
	protected final double[] sums;
	protected double[] eCache;
	protected double[] qCache;
	protected static final DecimalFormat df = new DecimalFormat("0.########");
	protected static final Instance zeroInstance = new Instance();
	
	public CD(int numCols, Config params, Loss loss, Regularization reg) {
		super(params, loss, reg);
		this.fm = new Model(numCols, params.nfactors, params.sigma);
		this.sums = new double[params.nfactors];
	}
	@Override
	public double[] learn(Data train, Data validation) {
		String title = "Data: " + train.getName() + "\n" + params;
		trace = title + "\n";
		System.out.println(title);
		fm.init(params.sigma);		// initialize the weight parameters
		eCache = new double[train.getNumRows()];
		qCache = new double[train.getNumRows()];
		long totalTime = 0;
		for (int n = 0; n < params.maxIter; n++) {
			long time = System.currentTimeMillis();
			update(train);
			time = System.currentTimeMillis() - time;
			totalTime += time;
			StringBuilder log = new StringBuilder();
			double[] predTrain = predict(train);
			double[] predVal = predict(validation);
			log.append(" Iteration ").append(n+1).append(":");
			log.append(" Train_loss=").append(df.format(loss(train, predTrain, false)));
			log.append(" Train_loss_weight=").append(df.format(loss(train, predTrain, true)));
			log.append(" Validation_loss=").append(df.format(loss(validation, predVal, false)));
			log.append(" Validation_loss_weight=").append(df.format(loss(validation, predVal, true)));
			log.append(" Time=").append(time).append("ms Total_time=").append(totalTime).append("ms");
			System.out.println(log.toString());
			trace += log.toString() + "\n";
		}
		return predict(validation);
	}
	protected void update(Data data) {
		int numRows = data.getNumRows();		
		if (eCache.length != numRows) {
			throw new IllegalArgumentException("Inconsistent size!");
		}
		double sumErr = 0.0;
		double sumSqr = (double) numRows;
		for (int i = 0; i < numRows; i++) {	// pre-compute cache e and q
			Instance x = data.row(i);
			eCache[i] = x.target - predict(x, sums);
			sumErr += eCache[i];
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
				double h = f.value;
				sumSqr += h * h;
				sumErr += h * eCache[f.index]; 
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
					double h = f.value * (qCache[f.index] - fm.V[k][j] * f.value); 
					sumSqr += h * h;
					sumErr += h * eCache[f.index]; 
					//scale += w;
				}
				//scale = (scale != 0.0)? (double) xt.size() / scale : 0.0;
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
	public double predict(Instance x, double[] sums) {
		return loss.transform(fm.output(x, sums));
	}
	public double[] predict(Data data) {
		double[] preds = new double[data.getNumRows()];
		for (int i = 0; i < data.getNumRows(); i++) {
			preds[i] = predict(data.row(i), sums);
		}
		return preds;
	}
	protected double loss(Data data, double[] preds) {
		if (data.getNumRows() != preds.length) {
			throw new IllegalArgumentException("Data size does not match prediction size");
		}
		double sumSqr = 0.0;
		double sumWeight = (double) preds.length;
		for (int i = 0; i < preds.length; i++) {
			Instance x = data.row(i);
			double diff = x.target - preds[i];
			sumSqr += diff * diff;
		}
		return Math.sqrt(sumSqr / sumWeight);
	}
}
