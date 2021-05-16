package larc.recommender.regularization;

import java.util.List;
import larc.recommender.hierarchy.Hierarchy;

public class HierL2 extends L2 {
	protected final Hierarchy hier;
	
	public HierL2(Hierarchy hier) {
		this.hier = hier;
	}
	@Override
	public double SGD(double[] weights, int idx, double eta, double lambda) {
		List<Integer> parents = hier.get(idx);
		double sum = weights[idx];
		if (parents != null) { 	// root node
			int numParents = parents.size();
			for (int pidx : parents) {
				sum -= weights[pidx] / numParents;
			}
		}
		return weights[idx] - eta * lambda * sum;
	}
	@Override
	public double CD(double[] weights, int idx, double lambda) {
		List<Integer> parents = hier.get(idx);
		if (parents == null || parents.size() == 0) { 	// root node
			return 0.0;
		}
		double sum = 0.0;
		for (int pidx : parents) {
			sum += weights[pidx];
		}
		return lambda * (sum / parents.size());
	}
	public Hierarchy getHierarchy() {
		return hier;
	}
	@Override
	public String toString() {
		return "HierarchicalL2";
	}
}
