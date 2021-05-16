package larc.recommender.data;

public class Feature implements Comparable<Feature> {
	public final int index;
	public final double value;
	
	public Feature(int index, double value) {
		this.index = index;
		this.value = value;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Feature other = (Feature) obj;
		return (index == other.index);
	}
	@Override
	public int compareTo(Feature that) {
		if (this.index < that.index) return -1;
		if (this.index > that.index) return 1;
		return 0;
	}
}
