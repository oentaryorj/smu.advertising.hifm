package larc.recommender.ads.test;

import larc.recommender.data.Data;
import larc.recommender.data.Instance;
import larc.recommender.evaluation.Ranking;
import larc.recommender.evaluation.Regression;

public class BaseRun {
	protected static String evaluate(Data data, double[] preds) {
		if (data.getNumRows() != preds.length) {
			throw new IllegalArgumentException("Mismatched size");
		}
		Ranking rank = new Ranking();
		Regression reg = new Regression(data.getNumRows());
		for (int i = 0; i < data.getNumRows(); i++) {
			Instance x = data.row(i);
			int click = (int) (x.target * x.weight);
			int expose = (int) x.weight;
			rank.add(preds[i], expose, click, x.shard);
			reg.add(x.target, preds[i], x.weight);
		}
		return reg + " " + rank;
	}
}
