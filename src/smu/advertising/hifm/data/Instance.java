package larc.recommender.data;

import java.util.Collections;
import java.util.LinkedList;

public class Instance extends LinkedList<Feature> {
	private static final long serialVersionUID = 1302345900276205431L;
	public double target = 0.0;
	public double weight = 1.0;
	public int shard = 0;
	public String id;
	
	public Instance() {
	}
	public Instance(Instance that) {
		super(that);
	}
	@Override
	public boolean add(Feature f) {
		/*for (Feature x : this) {
			if (x.index == f.index) return false;
		}*/
		return super.add(f);
	}
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(this.target).append(" ");
		Collections.sort(this);
		for (Feature f : this) {
			str.append(f.index).append(":").append(f.value).append(" ");
		}
		return str.toString().trim();
	}
}
