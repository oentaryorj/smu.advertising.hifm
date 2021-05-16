package larc.recommender.learn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import larc.recommender.base.Config;
import larc.recommender.base.Model;
import larc.recommender.base.Pair;
import larc.recommender.data.Data;
import larc.recommender.data.Feature;
import larc.recommender.data.Instance;
import larc.recommender.loss.Loss;
import larc.recommender.random.RandGen;
import larc.recommender.random.RouletteWheel;
import larc.recommender.regularization.Regularization;

public class ASGD extends SGD {
	protected Model localFM;
	protected double miu = 0.0;
	
	public ASGD(int numCols, Config params, Loss loss, Regularization reg) {
		super(numCols, params, loss, reg);
		this.localFM = new Model(numCols, params.nfactors, params.sigma);
	}
	@Override
	public double[] learn(Data train, Data validation) {
		String title = "Data: " + train.getName() + "\n" + params;
		trace = title + "\n";
		System.out.println(title);
		localFM.init(params.sigma);		// initialize the weight parameters
		if (params.alpha < 1.0) {		// construct candidate pairs
			buildPairs(train);
		}
		int numRows = train.getNumRows();
		double[] sampleWeights = new double[numRows];
		for (int i = 0; i < numRows; i++) {
			sampleWeights[i] = train.row(i).weight;
		}
		RouletteWheel rw = new RouletteWheel(sampleWeights);
		double eta = params.eta;
		long totalTime = 0;
		long iter = 0;
		for (int n = 0; n < params.maxIter; n++) {
			long time = System.currentTimeMillis();
			for (int i = 0; i < numRows; i++) {
				miu = Math.max(params.eta, 1.0 / ++iter);
				Instance x = train.row(rw.select());
				if (RandGen.uniform() <= params.alpha) { //|| candidates.size() == 0) {		// optimize regression loss 
					update(x, eta, eta, eta);
				}
				else {									// optimize ranking loss
					List<Instance> candidates = candidatePairs.get(x);
					if (candidates.size() > 0) {
						Instance xp = candidates.get(rand.nextInt(candidates.size()));
						updatePair(x, xp, eta, eta);
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
		double eta = params.eta;
		long totalTime = 0;
		long iter = 0;
		for (int n = 0; n < params.maxIter; n++) {
			long time = System.currentTimeMillis();
			for (int i = 0; i < numRows; i++) {
				miu = Math.max(params.eta, 1.0 / ++iter);
				Instance xpos = positives.get(posRW.select());
				Instance xneg = negatives.get(negRW.select());
				if (RandGen.uniform() <= params.alpha) { //|| candidates.size() == 0) {		// optimize regression loss 
					update(xpos, eta, eta, eta);
					update(xneg, eta, eta, eta);
				}
				else {									// optimize ranking loss
					updatePair(xpos, xneg, eta, eta);
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
	@Override
	protected void update(Instance x, double eta0, double etaW, double etaV) {
		double grad = loss.gradient(localFM.output(x, sums), x.target);
		if (grad == 0.0) {
			return;
		}
		localFM.w0 = reg.SGD(localFM.w0, eta0, params.reg0) - eta0 * grad;
		fm.w0 += miu * (localFM.w0 - fm.w0);
		for (Feature f : x) {
			int j = f.index;
			double x_j = f.value;
			localFM.W[j] = reg.SGD(localFM.W, j, etaW, params.regW) - etaW * grad * x_j;
			fm.W[j] += miu * (localFM.W[j] - fm.W[j]);
			for (int k = 0; k < params.nfactors; k++) {
				localFM.V[k][j] = reg.SGD(localFM.V[k], j, etaV, params.regV) - etaV * grad * x_j * (sums[k] - fm.V[k][j] * x_j);
				fm.V[k][j] += miu * (localFM.V[k][j] - fm.V[k][j]);
			}
		}
	}
	@Override
	protected void updatePair(Instance xa, Instance xb, double etaW, double etaV) {
		double grad = loss.pairGradient(localFM.output(xa, sums_a), localFM.output(xb, sums_b), xa.target, xb.target);
		if (grad == 0.0) {
			return;
		}
		//localFM.w0 -= eta0 * 0.0;
		//fm.w0 += miu * (localFM.w0 - fm.w0);
		for (Entry<Integer, Pair<Double>> e : unify(xa, xb).entrySet()) {
			int j = e.getKey();
			Pair<Double> pair = e.getValue();
			double xa_j = pair.getFirst();
			double xb_j = pair.getSecond();
			localFM.W[j] = reg.SGD(localFM.W, j, etaW, params.regW) - etaW * grad * (xa_j - xb_j);
			fm.W[j] += miu * (localFM.W[j] - fm.W[j]);
			for (int k = 0; k < params.nfactors; k++) {
				double a = xa_j * (sums_a[k] - fm.V[k][j] * xa_j);
				double b = xb_j * (sums_b[k] - fm.V[k][j] * xb_j);
				localFM.V[k][j] = reg.SGD(localFM.V[k], j, etaV, params.regV) - etaV * grad * (a - b);
				fm.V[k][j] += miu * (localFM.V[k][j] - fm.V[k][j]);
			}
		}
	}
}