package larc.recommender.learn;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import larc.recommender.base.CandidatePairs;
import larc.recommender.base.Config;
import larc.recommender.base.Model;
import larc.recommender.base.Pair;
import larc.recommender.data.Data;
import larc.recommender.data.Feature;
import larc.recommender.data.Instance;
import larc.recommender.hierarchy.Hierarchy;
import larc.recommender.loss.Loss;
import larc.recommender.random.RandGen;
import larc.recommender.random.RouletteWheel;
import larc.recommender.regularization.HierL2;
import larc.recommender.regularization.Regularization;

public class SGD extends Learn {
	protected final double[] sums;
	protected final double[] sums_a;
	protected final double[] sums_b;
	protected CandidatePairs candidatePairs;
	protected static final DecimalFormat df = new DecimalFormat("0.########");
	
	public SGD(int numCols, Config params, Loss loss, Regularization reg) {
		super(params, loss, reg);
		this.fm = new Model(numCols, params.nfactors, params.sigma);
		this.candidatePairs = new CandidatePairs();
		this.sums = new double[params.nfactors];
		this.sums_a = new double[params.nfactors];
		this.sums_b = new double[params.nfactors];
	}
	@Override
	public double[] predict(Data data) {
		double[] preds = new double[data.getNumRows()];
		for (int i = 0; i < data.getNumRows(); i++) {
			preds[i] = predict(data.row(i), sums);
		}
		return preds;
	}
	public double predict(Instance x, double[] sums) {
		return loss.transform(fm.output(x, sums));
	}
	@Override
	public double[] learn(Data train, Data validation) {
		String title = "Data: " + train.getName() + "\n" + params;
		trace = title + "\n";
		System.out.println(title);
		fm.init(params.sigma);		// initialize the weight parameters
		rectifyColdStart(train);	// reinitialize weights to zero for cold-start situations
		if (params.alpha < 1.0) {	// construct candidate pairs
			buildPairs(train);
		}
		int numRows = train.getNumRows();
		double[] sampleWeights = new double[numRows];
		for (int i = 0; i < numRows; i++) {
			sampleWeights[i] = 1.0;
		}
		RouletteWheel rw = new RouletteWheel(sampleWeights);
		double eta = params.eta;
		long totalTime = 0;
		for (int n = 0; n < params.maxIter; n++) {
			long time = System.currentTimeMillis();
			for (int i = 0; i < numRows; i++) {
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
		augmentParents(train);		// handling cold-start situations
		candidatePairs.clear();		// release memory
		return predict(validation);
	}
	public double[] learnBalance(Data train, Data validation) {
		String title = "Data: " + train.getName() + "\n" + params;
		trace = title + "\n";
		System.out.println(title);
		fm.init(params.sigma);		// initialize the weight parameters
		rectifyColdStart(train);	// reinitialize weights to zero for cold-start situations
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
			posWeights[i] = 1.0;
		}
		for (int i = 0; i < negatives.size(); i++) {
			negWeights[i] = 1.0;
		}
		RouletteWheel posRW = new RouletteWheel(posWeights);
		RouletteWheel negRW = new RouletteWheel(negWeights);
		double eta = params.eta;
		long totalTime = 0;
		for (int n = 0; n < params.maxIter; n++) {
			long time = System.currentTimeMillis();
			for (int i = 0; i < numRows; i++) {
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
		augmentParents(train);		// handling cold-start situations
		candidatePairs.clear();		// release memory
		return predict(validation);
	}
	protected void update(Instance x, double eta0, double etaW, double etaV) {
		double grad = loss.gradient(fm.output(x, sums), x.target);
		if (grad == 0.0) {
			return;
		}
		fm.w0 = reg.SGD(fm.w0, eta0, params.reg0) - eta0 * grad;
		for (Feature f : x) {
			int j = f.index;
			double x_j = f.value;
			fm.W[j] = reg.SGD(fm.W, j, etaW, params.regW) - etaW * grad * x_j;
			for (int k = 0; k < params.nfactors; k++) {
				fm.V[k][j] = reg.SGD(fm.V[k], j, etaV, params.regV) - etaV * grad * x_j * (sums[k] - fm.V[k][j] * x_j);
			}
		}
	}
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
			fm.W[j] = reg.SGD(fm.W, j, etaW, params.regW) - etaW * grad * (xa_j - xb_j);
			for (int k = 0; k < params.nfactors; k++) {
				double a = xa_j * (sums_a[k] - fm.V[k][j] * xa_j);
				double b = xb_j * (sums_b[k] - fm.V[k][j] * xb_j);
				fm.V[k][j] = reg.SGD(fm.V[k], j, etaV, params.regV) - etaV * grad * (a - b); 
			}
		}
	}
	protected void buildPairs(Data data) {
		long time = System.currentTimeMillis();
		TreeMap<Integer, ArrayList<Instance>> shardMap = new TreeMap<Integer, ArrayList<Instance>>();
		candidatePairs.clear();
		for (Instance x : data.get()) {
			ArrayList<Instance> list = shardMap.get(x.shard);		
			if (list == null) {
				list = new ArrayList<Instance>();
			}
			list.add(x);
			shardMap.put(x.shard, list);
		}
		for (Entry<Integer, ArrayList<Instance>> e : shardMap.entrySet()) {
			ArrayList<Instance> instances = e.getValue();
			for (Instance x : instances) {
				ArrayList<Instance> candidates = new ArrayList<Instance>();
				for (Instance xp : instances) {
					if (Double.doubleToLongBits(x.target) != Double.doubleToLongBits(xp.target)) {
						candidates.add(xp);
					}
				}
				candidatePairs.put(x, candidates);
			}
		}
		System.out.println("Building rank pairs takes " + (System.currentTimeMillis() - time) + " ms");
	}
	protected void rectifyColdStart(Data data) {
		for (int j = 0; j < data.getNumCols(); j++) {	
			if (data.col(j).size() == 0) { 	// zero all parameters in the same column in cold-start event
				fm.W[j] = 0.0;
				for (int k = 0; k < params.nfactors; k++) {
					fm.V[k][j] = 0.0;
				}
			}
		}
	}
	protected void augmentParents(Data data) {
		if (!(reg instanceof HierL2)) {
			return;
		}
		Hierarchy hier = ((HierL2) reg).getHierarchy();
		for (int j = 0; j < data.getNumCols(); j++) {
			if (data.col(j).size() == 0) {
				fm.W[j] = 0.0;		// reset all parameters to zero
				for (int k = 0; k < params.nfactors; k++) {
					fm.V[k][j] = 0.0;
				}
				augmentParentsStub(hier, j);
			}
		}
	}
	private void augmentParentsStub(Hierarchy hier, int j) {
		List<Integer> parents = hier.get(j);
		if (parents == null || parents.size() == 0) {
			return;
		}
		int numParents = parents.size();
		for (int p : parents) {
			fm.W[j] += fm.W[p] / numParents;
			for (int k = 0; k < params.nfactors; k++) {
				fm.V[k][j] += fm.V[k][p] / numParents;
			}
			augmentParentsStub(hier, p);
		} 
	}
}