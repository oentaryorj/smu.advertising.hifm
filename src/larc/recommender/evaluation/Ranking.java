package larc.recommender.evaluation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Ranking {
	private final List<Double> predictions = new ArrayList<Double>();
	private final List<Integer> exposes = new ArrayList<Integer>();
	private final List<Integer> clicks = new ArrayList<Integer>();
	private final List<Integer> shards = new ArrayList<Integer>();

	public Ranking() {
	}
	public void add(double prediction, int expose, int click, int shard) {
		predictions.add(prediction);
		exposes.add(expose);
		clicks.add(click);
		shards.add(shard);
	}
	public void clear() {
		predictions.clear();
		exposes.clear();
		clicks.clear();
		shards.clear();
	}
	public int size() {
		return predictions.size();
	}
	// Area under ROC curve (weighted by exposes)
	public double AUC() {		
		IndexComparator comparator = new IndexComparator(predictions, false);	// sort in descending order
		Integer[] i_sorted = comparator.createIndices();
		Arrays.sort(i_sorted, comparator);
		double auc_temp = 0.0;
	    double click_sum = 0.0;
	    double old_click_sum = 0.0;
	    double no_click = 0.0;
	    double no_click_sum = 0.0;
	    double last_ctr = predictions.get(i_sorted[0]) + 1.0;
	    for (int i = 0; i < predictions.size(); i++) {
	        if (last_ctr != predictions.get(i_sorted[i])) {
	            auc_temp += 0.5 * (double) ((click_sum + old_click_sum) * no_click);        
	            old_click_sum = click_sum;
	            no_click = 0.0;
	            last_ctr = predictions.get(i_sorted[i]);
	        }
	        no_click += exposes.get(i_sorted[i]) - clicks.get(i_sorted[i]);
	        no_click_sum += exposes.get(i_sorted[i]) - clicks.get(i_sorted[i]);
	        click_sum += clicks.get(i_sorted[i]);
	    }
	    auc_temp += 0.5 * (click_sum + old_click_sum) * no_click;
	    return (click_sum > 0.0 && no_click_sum > 0.0)? (auc_temp / (click_sum * no_click_sum)) : 0.0;
	}
	// Mean reciprocal rank
	public double MRR() {
		ArrayList<Double> preds = new ArrayList<Double>();
		ArrayList<Double> tars = new ArrayList<Double>();
		double sumRecRank = 0.0;
		Map<Integer, List<Integer>> group = groupByShard();
		for (Entry<Integer, List<Integer>> e : group.entrySet()) {
			preds.clear();	// reset
			tars.clear();
			for (Integer i : e.getValue()) {
				preds.add(predictions.get(i));
				double d = (double) exposes.get(i);
				tars.add((d != 0.0)? ((double) clicks.get(i) / d) : 0.0);
			}
			IndexComparator predComp = new IndexComparator(preds, false);	// sort in descending order
			Integer[] p_sorted = predComp.createIndices();
			Arrays.sort(p_sorted, predComp);
			for (int i = 0; i < tars.size(); i++) {
				if (tars.get(p_sorted[i]) > 0.0) {
					sumRecRank += 1.0 / (double) (i + 1);
					break;
				}
			} 
		}
		return sumRecRank / (double) group.size();
	}
	// Mean average precision at k
	public double MAP(int k) {
		if (k < 1) {
			throw new IllegalArgumentException("Rank k must be positive integer");
		}
		ArrayList<Double> preds = new ArrayList<Double>();
		ArrayList<Double> tars = new ArrayList<Double>();
		double sumAvgPrec = 0.0, sumWeight = 0.0;
		Map<Integer, List<Integer>> group = groupByShard();
		for (Entry<Integer, List<Integer>> e : group.entrySet()) {
			preds.clear();	// reset
			tars.clear();
			double weight = 0.0;
			for (Integer i : e.getValue()) {
				preds.add(predictions.get(i));
				double ne = (double) exposes.get(i);
				tars.add((ne != 0.0)? ((double) clicks.get(i) / ne) : 0.0);
				weight += ne;
			}
			sumAvgPrec += weight * AP(tars, preds, k);
			sumWeight += weight;
		}
		return sumAvgPrec / sumWeight;
	}
	// Average precision at k
	public static double AP(List<Double> targets, List<Double> preds, int k) {	
		if (k < 1) {
			throw new IllegalArgumentException("Rank k must be positive integer");
		} else if (targets.size() != preds.size() || targets.size() == 0 || preds.size() == 0) {
			throw new IllegalArgumentException("Target size is not equal to prediction size");
		}
		IndexComparator predComp = new IndexComparator(preds, false);	// sort in descending order
		IndexComparator tarComp = new IndexComparator(targets, false);		// sort in descending order
		Integer[] p_sorted = predComp.createIndices();
		Integer[] t_sorted = tarComp.createIndices();
		Arrays.sort(p_sorted, predComp);
		Arrays.sort(t_sorted, tarComp);
		boolean[] tarLabels = new boolean[targets.size()];
		int numRelevants = 0;
		for (int i = 0; i < tarLabels.length; i++) {			// derive the ground-truth labels
			//tarLabels[t_sorted[i]] = (i < topK);
			if (targets.get(i) > 0.0) {
				tarLabels[i] = true;
				numRelevants++;
			} else {
				tarLabels[i] = false;
			}
		}
		if (numRelevants == 0) {	// when there is no relevant document at all
			return 0.0;
		}
		double sumPrec = 0.0;
		int correct = 0;
		for (int i = 0; i < Math.min(k, tarLabels.length); i++) {
			if (tarLabels[p_sorted[i]]) {
				++correct;
				sumPrec += (double) correct / (double) (i + 1);
			}
		}
		return sumPrec /  Math.min(numRelevants, k);
	}
	// Normalized discounted cumulative gain
	public double NDCG(int k) {
		if (k < 1) {
			throw new IllegalArgumentException("Rank k must be positive integer");
		}
		ArrayList<Double> preds = new ArrayList<Double>();
		ArrayList<Double> tars = new ArrayList<Double>();
		double sumNDCG = 0.0;
		Map<Integer, List<Integer>> group = groupByShard();
		for (Entry<Integer, List<Integer>> e : group.entrySet()) {
			preds.clear();	// reset
			tars.clear();
			for (Integer i : e.getValue()) {
				preds.add(predictions.get(i));
				double d = (double) exposes.get(i);
				tars.add((d != 0.0)? ((double) clicks.get(i) / d) : 0.0);
			}
			IndexComparator predComp = new IndexComparator(preds, false);	// sort in descending order
			IndexComparator tarComp = new IndexComparator(tars, false);		// sort in descending order
			Integer[] p_sorted = predComp.createIndices();
			Integer[] t_sorted = tarComp.createIndices();
			Arrays.sort(p_sorted, predComp);
			Arrays.sort(t_sorted, tarComp);
			double DCG = 0.0, idealDCG = 0.0;
			int rank = Math.min(k, tars.size());
			for (int i = 0; i < rank; i++) {
				double scale = (i > 0)? (Math.log(2.0) / Math.log(i+1)) : 1.0;
				DCG += scale * tars.get(p_sorted[i]);
				idealDCG += scale * tars.get(t_sorted[i]);
			}
			if (DCG > idealDCG) {
				throw new IllegalArgumentException("DCG is greater than ideal DCG!");
			}
			sumNDCG += (idealDCG != 0.0)? (DCG / idealDCG) : 0.0;
		}
		return sumNDCG / (double) group.size();
	}
	// Kendall's rank correlation
	public double rankCorrel() {
		ArrayList<Double> preds = new ArrayList<Double>();
		ArrayList<Double> tars = new ArrayList<Double>();
		double rankCorrel = 0.0;
		Map<Integer, List<Integer>> group = groupByShard();
		for (Entry<Integer, List<Integer>> e : group.entrySet()) {
			preds.clear();	// reset
			tars.clear();
			for (Integer i : e.getValue()) {
				preds.add(predictions.get(i));
				double d = (double) exposes.get(i);
				tars.add((d != 0.0)? ((double) clicks.get(i) / d) : 0.0);
			}
			int size = e.getValue().size();
			if (size < 2) {
				continue;
			}
			int concordant = 0, discordant = 0;
			for (int i = 0; i < size; i++) {
				double pi = preds.get(i);
				double ti = tars.get(i);
				for (int j = i + 1; j < size; j++) {
					double pj = preds.get(j);
					double tj = tars.get(j);
					if (ti != tj && pi != pj) {
						if ((ti > tj && pi > pj) || (ti < tj && pi < pj)) {
							concordant++;
						} else {
							discordant++;
						}
					}
				}
			}
			rankCorrel += (concordant - discordant) / (0.5 * size * (size - 1));
		} 
		return rankCorrel / (double) group.size();
	}
	private Map<Integer, List<Integer>> groupByShard() {
		HashMap<Integer, List<Integer>> group = new HashMap<Integer, List<Integer>>();
		for (int i = 0; i < predictions.size(); i++) {
			int shard = shards.get(i);
			List<Integer> list = group.get(shard);
			if (list == null) {
				list = new LinkedList<Integer>();
			}
			list.add(i);
			group.put(shard, list);
		}
		return group;
	}
	// Pearson correlation (single-pass algorithm)
	/*private static double correl(Integer[] x, Integer[] y) {
		if (x.length != y.length) {
			throw new IllegalArgumentException("x size is not equal to y size");
		}
		double sum_sq_x = 0.0, sum_sq_y = 0.0;
		double sum_coproduct = 0.0;
		double mean_x = x[0];
		double mean_y = y[0];
		for (int i = 1; i < x.length; i++) {
			double sweep = (double) i / (double) (i + 1);
			double delta_x = x[i] - mean_x;
			double delta_y = y[i] - mean_y;
			sum_sq_x += delta_x * delta_x * sweep;
			sum_sq_y += delta_y * delta_y * sweep;
			sum_coproduct += delta_x * delta_y * sweep;
			mean_x += delta_x / (i + 1);
			mean_y += delta_y / (i + 1);
		}
		return (sum_sq_x != 0.0 && sum_sq_y != 0.0)? (sum_coproduct / Math.sqrt(sum_sq_x * sum_sq_y)) : 0.0;
	}*/
	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("0.########");
		StringBuilder str = new StringBuilder();
		str.append("AUC=").append(df.format(AUC()));
		str.append(" MAP@3=").append(df.format(MAP(3)));
		str.append(" NDCG@3=").append(df.format(NDCG(3)));
		str.append(" MRR=").append(df.format(MRR()));
		//str.append(" RC=").append(df.format(rankCorrel()));
		return str.toString().trim();
	}
}
