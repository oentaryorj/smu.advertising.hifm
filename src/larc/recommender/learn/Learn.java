package larc.recommender.learn;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import larc.recommender.base.Config;
import larc.recommender.base.Model;
import larc.recommender.base.Pair;
import larc.recommender.data.Data;
import larc.recommender.data.Feature;
import larc.recommender.data.Instance;
import larc.recommender.loss.Loss;
import larc.recommender.loss.SquareLoss;
import larc.recommender.regularization.Regularization;

public abstract class Learn {
	protected final Random rand = new Random(0);
	protected final Loss loss;
	protected final Regularization reg;
	protected final Config params;	// hyperparameters
	protected Model fm;				// factorization machine model
	protected String trace;			// learning trace
	
	public Learn(Config params, Loss loss, Regularization reg) {
		this.params = params;
		this.loss = loss;
		this.reg = reg;
	}
	public Config getParams() {
		return params;
	}
	public Model getModel() {
		return fm;
	}
	public String getTrace() {
		return trace;
	}
	public abstract double[] learn(Data train, Data validation);
	public abstract double[] predict(Data data);
	
	protected static Map<Integer, Pair<Double>> unify(Instance xa, Instance xb) {
		TreeMap<Integer, Pair<Double>> union = new TreeMap<Integer, Pair<Double>>();
		for (Feature f : xa) {
			union.put(f.index, new Pair<Double>(f.value, 0.0));
		}
		for (Feature f : xb) {
			Pair<Double> pair = union.get(f.index);
			if (pair == null) pair = new Pair<Double>(0.0, 0.0); 
			pair.setSecond(f.value); 
			union.put(f.index, pair);
		}
		return union;
	}
	protected double loss(Data data, double[] preds, boolean weighted) {
		if (data.getNumRows() != preds.length) {
			throw new IllegalArgumentException("Data size does not match prediction size");
		}
		double sumLoss = 0.0, sumWeight = 0.0;
		if (weighted) {
			for (int i = 0; i < preds.length; i++) {
				Instance x = data.row(i);
				sumLoss += loss.get(preds[i], x.target) * x.weight;
				sumWeight += x.weight;
			}
		} else {
			for (int i = 0; i < preds.length; i++) {
				sumLoss += loss.get(preds[i], data.row(i).target);
			}
			sumWeight = preds.length;
		}
		if (loss instanceof SquareLoss) {
			return Math.sqrt(sumLoss / sumWeight);
		} else {
			return (sumLoss / sumWeight);
		}
	}
	/*// Augment parents information from the hierarchy
	public Data augment(Data data, Hierarchy hier) {
		int numRows = data.getNumRows();
		int numCols = data.getNumCols();
		for (Entry<Integer, List<Integer>> e : hier.entrySet()) {
			for (Integer j : e.getValue()) {
				numCols = Math.max(numCols, j + 1);
			}
		}
		Data extData = new Data(numRows, numCols);
		Set<Integer> ancestors = new TreeSet<Integer>();
		for (int i = 0; i < numRows; i++) {
			Instance x = data.row(i);
			Instance xe = extData.row(i);
			xe.id = x.id;
			xe.shard = x.shard;
			xe.target = x.target;
			xe.weight = x.weight;
			for (Feature f : x) {
				int j = f.index;
				ancestors.clear();	// reset
				addParents(j, hier, ancestors);
				extData.set(i, j, 0.5 * f.value);
				double value = 0.5 * f.value / (double) ancestors.size();
				for (Integer p : ancestors) {
					extData.set(i, p, value);		// augment new features
				}
			}
		}
		return extData;
	}
	// Depth-first search procedure to augment parent nodes
	private static void addParents(int child, Hierarchy hier, Set<Integer> ancestors) {
		List<Integer> parents = hier.get(child);
		if (parents == null || parents.size() == 0) {
			return;
		} 
		for (Integer p : parents) {
			ancestors.add(p);
			addParents(p, hier, ancestors);
		}
	}*/
}
