package larc.recommender.learn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import larc.recommender.base.Config;
import larc.recommender.base.Pair;
import larc.recommender.data.Data;
import larc.recommender.data.Feature;
import larc.recommender.data.Instance;
import larc.recommender.loss.Loss;
import larc.recommender.random.RandGen;
import larc.recommender.random.RouletteWheel;
import larc.recommender.regularization.Regularization;

public class PSGD extends SGD {
	protected double sqNorm = 0.0;
	
	public PSGD(int numCols, Config params, Loss loss, Regularization reg) {
		super(numCols, params, loss, reg);
		sqNorm = fm.w0 * fm.w0;
		for (int j = 0; j < fm.W.length; j++) {
			sqNorm += fm.W[j] * fm.W[j];					// initialize W to zero
			for (int k = 0; k < fm.V.length; k++) {		
				sqNorm += fm.V[k][j] * fm.V[k][j]; 			// initialize V such that V ~ N(0, sigma)
			}
		}
	}
	@Override
	protected void update(Instance x, double eta0, double etaW, double etaV) {
		double grad = loss.gradient(fm.output(x, sums), x.target);
		if (grad == 0.0) {
			return;
		}
		double[] w0 = {fm.w0};
		double newW0 = reg.SGD(w0, 0, eta0, params.reg0) - eta0 * grad;
		sqNorm += newW0 * newW0 - fm.w0 * fm.w0;
		fm.w0 = newW0;
		for (Feature f : x) {
			int j = f.index;
			double x_j = f.value;
			double newW = reg.SGD(fm.W, j, etaW, params.regW) - etaW * grad * x_j;
			sqNorm += newW * newW - fm.W[j] * fm.W[j];
			fm.W[j] = newW;
			for (int k = 0; k < params.nfactors; k++) {
				double newV = reg.SGD(fm.V[k], j, etaV, params.regV) - etaV * grad * x_j * (sums[k] - fm.V[k][j] * x_j);
				sqNorm += newV * newV - fm.V[k][j] * fm.V[k][j];
				fm.V[k][j] = newV;
			}
		}
		projectToL2Ball();
	}
	@Override
	protected void updatePair(Instance xa, Instance xb, double etaW, double etaV) {
		double grad = loss.pairGradient(fm.output(xa, sums_a), fm.output(xb, sums_b), xa.target, xb.target);
		if (grad == 0.0) {
			return;
		}
		//fm.w0 -= eta0 * 0.0;
		for (Entry<Integer, Pair<Double>> e : unify(xa, xb).entrySet()) {
			int j = e.getKey();
			Pair<Double> pair = e.getValue();
			double xa_j = pair.getFirst();
			double xb_j = pair.getSecond();
			double newW = reg.SGD(fm.W, j, etaW, params.regW) - etaW * grad * (xa_j - xb_j);
			sqNorm += newW * newW - fm.W[j] * fm.W[j];
			fm.W[j] = newW;
			for (int k = 0; k < params.nfactors; k++) {
				double a = xa_j * (sums_a[k] - fm.V[k][j] * xa_j);
				double b = xb_j * (sums_b[k] - fm.V[k][j] * xb_j);
				double newV = reg.SGD(fm.V[k], j, etaV, params.regV) - etaV * grad * (a - b);
				sqNorm += newV * newV - fm.V[k][j] * fm.V[k][j];
				fm.V[k][j] = newV;
			}
		}
		projectToL2Ball();
	}
	@Override
	public double[] learn(Data train, Data validation) {
		String title = "Data: " + train.getName() + "\n" + params;
		trace = title + "\n";
		System.out.println(title);
		fm.init(params.sigma);		// initialize the weight parameters
		if (params.alpha < 1.0) {	// construct candidate pairs
			buildPairs(train);
		}
		int numRows = train.getNumRows();
		double[] sampleWeights = new double[numRows];
		for (int i = 0; i < numRows; i++) {
			sampleWeights[i] = train.row(i).weight;
		}
		RouletteWheel rw = new RouletteWheel(sampleWeights);
		long totalTime = 0;
		long iter = 0;
		for (int n = 0; n < params.maxIter; n++) {
			long time = System.currentTimeMillis();
			for (int i = 0; i < numRows; i++) {
				++iter;
				Instance x = train.row(rw.select());
				if (RandGen.uniform() <= params.alpha) { //|| candidates.size() == 0) {		// optimize regression loss 
					update(x, 1.0 / (params.reg0 * iter), 1.0 / (params.regW * iter), 1.0 / (params.regV * iter));
				}
				else {									// optimize ranking loss
					List<Instance> candidates = candidatePairs.get(x);
					if (candidates.size() > 0) {
						Instance xp = candidates.get(rand.nextInt(candidates.size()));
						updatePair(x, xp, 1.0 / (params.regW * iter), 1.0 / (params.regV * iter));
					}
				}
			}
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
		candidatePairs.clear();		// release memory
		return predict(validation);
	}
	@Override
	public double[] learnBalance(Data train, Data validation) {
		String title = "Data: " + train.getName() + "\n" + params;
		trace = title + "\n";
		System.out.println(title);
		fm.init(params.sigma);		// initialize the weight parameters
		ArrayList<Instance> positives = new ArrayList<Instance>();
		ArrayList<Instance> negatives = new ArrayList<Instance>();
		for (Instance x : train.get()) { 
			if (x.target > 0.0) {
				positives.add(x);
			} else {
				negatives.add(x);
			}
		}
		int numRows = train.getNumRows();
		double[] posWeights = new double[positives.size()];
		double[] negWeights = new double[positives.size()];
		for (int i = 0; i < positives.size(); i++) {
			posWeights[i] = positives.get(i).weight;
		}
		for (int i = 0; i < negatives.size(); i++) {
			negWeights[i] = negatives.get(i).weight;
		}
		RouletteWheel posRW = new RouletteWheel(posWeights);
		RouletteWheel negRW = new RouletteWheel(negWeights);
		long totalTime = 0;
		long iter = 0;
		for (int n = 0; n < params.maxIter; n++) {
			long time = System.currentTimeMillis();
			for (int i = 0; i < numRows; i++) {
				++iter;
				Instance xpos = positives.get(posRW.select());
				Instance xneg = negatives.get(negRW.select());
				if (RandGen.uniform() <= params.alpha) { //|| candidates.size() == 0) {		// optimize regression loss 
					update(xpos, 1.0 / (params.reg0 * iter), 1.0 / (params.regW * iter), 1.0 / (params.regV * iter));
					update(xneg, 1.0 / (params.reg0 * iter), 1.0 / (params.regW * iter), 1.0 / (params.regV * iter));
				}
				else {									// optimize ranking loss
					updatePair(xpos, xneg, 1.0 / (params.regW * iter), 1.0 / (params.regV * iter));
				}
			}
			time = System.currentTimeMillis() - time;
			totalTime += time;
			StringBuilder log = new StringBuilder();
			log.append(" Iteration ").append(n+1).append(":");
			log.append(" Train_loss=").append(df.format(loss(train, predict(train), false)));
			log.append(" Train_loss_weight=").append(df.format(loss(train, predict(train), true)));
			log.append(" Validation_loss=").append(df.format(loss(validation, predict(validation), false)));
			log.append(" Validation_loss_weight=").append(df.format(loss(validation, predict(validation), true)));
			log.append(" Time=").append(time).append("ms Total_time=").append(totalTime).append("ms");
			System.out.println(log.toString());
			trace += log.toString() + "\n";
		}
		return predict(validation);
	}
	private void projectToL2Ball() {
		double scale = 1.0 / Math.sqrt(params.regW * sqNorm);
		if (scale < 1.0) {
			fm.w0 *= scale;
			for (int j = 0; j < fm.W.length; j++) {
				fm.W[j] *= scale;
				for (int k = 0; k < fm.V.length; k++) {	
					fm.V[k][j] *= scale; 				// initialize V such that V ~ N(0, sigma)
				}
			}
			sqNorm *= scale * scale;
		}
	}
}
