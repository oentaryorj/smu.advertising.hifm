package larc.recommender.ads.db;

public class CTR {
	public long expose;
	public long click;
	
	public CTR(long expose, long click) {
		this.expose = expose;
		this.click = click;
	}
	public double value() {
		return (this.expose != 0)? (double) this.click / (double) this.expose : 0.0;
	}
	public CTR add(CTR that) {
		this.expose += that.expose;
		this.click += that.click;
		return this;
	}
	@Override
	public String toString() {
		return Double.toHexString(this.value());
	}
}
