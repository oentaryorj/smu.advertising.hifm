package larc.recommender.hierarchy;

public class Node implements Comparable<Node>{
	public final String id;
	public long expose;
	public long click;
	
	public Node(String id) {
		this.id = id;
		this.expose = this.click = 0;
	}
	public Node(String id, long expose, long click) {
		this.id = id;
		this.expose = expose;
		this.click = click;
	}
	public Node add(long expose, long click) {
		this.expose += expose;
		this.click += click;
		return this;
	}
	public double value() {
		return (expose != 0)? ((double) click / (double) expose) : 0.0;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (click ^ (click >>> 32));
		result = prime * result + (int) (expose ^ (expose >>> 32));
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Node other = (Node) obj;
		return id.equals(other.id);
	}
	@Override
	public int compareTo(Node that) {
		return id.compareTo(that.id);
	}
	@Override
	public String toString() {
		return id + "," + expose + "," + click;
	}
}
