package larc.recommender.ads.db;

public class Range {
	public final String start;
	public final String end;
	
	public Range(String s, String e) {
		this.start = s;
		this.end = (s.compareTo(e) <= 0)? e : s;
	}
	public boolean isWithin(String query) {
		return query.compareTo(start) >= 0 && query.compareTo(end) <= 0;
	}
}
