package larc.recommender.learn;

import java.util.ArrayList;
import java.util.List;

import larc.recommender.base.Config;
import larc.recommender.data.Data;
import larc.recommender.data.Instance;
import larc.recommender.loss.Loss;
import larc.recommender.random.RandGen;
import larc.recommender.random.RouletteWheel;
import larc.recommender.regularization.Regularization;

public class WSGD extends SGD {
	public WSGD(int numCols, Config params, Loss loss, Regularization reg) {
		super(numCols, params, loss, reg);
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
			sampleWeights[i] = train.row(i).weight;
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
	@Override
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
			posWeights[i] = positives.get(i).weight;
		}
		for (int i = 0; i < negatives.size(); i++) {
			negWeights[i] = negatives.get(i).weight;
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
}