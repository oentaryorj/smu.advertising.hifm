package larc.recommender.ads.db;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public abstract class CTRExtractor {
	protected static final Charset charSet = Charset.forName("UTF-8");
	protected final Map<String, CTR> data = new TreeMap<String, CTR>();					// the matrix/tensor data
	protected final Map<String, CTR> selected = new TreeMap<String, CTR>();				// the matrix/tensor data
	protected final Map<String, Integer> userSet = new TreeMap<String, Integer>();		// mapping from user ID to row index
	protected final Map<String, Integer> itemSet = new TreeMap<String, Integer>();		// mapping from item ID to col index
	protected final Map<String, Integer> timeSet = new TreeMap<String, Integer>();		// mapping from item ID to col index
	protected static final Range dummyRange = new Range("1970-01-01", "1970-01-01");
	
	public abstract Map<String, CTR> readSQL(String schema, String pageTable, String adTable, Range dayRange, int minExpose) throws Exception;
	public abstract void writeEdge(String file, String delim, String header) throws IOException;
	public abstract void writeSparse(String file, String delim) throws IOException;
	public abstract void writeShard(String file) throws IOException;
	public abstract void writeEntity(String file) throws IOException;
	public abstract Map<String, CTR> filter(Range dayRange);
	
	public void clear() {
		this.data.clear();
		this.userSet.clear();
		this.itemSet.clear();
		this.timeSet.clear();
	}
	protected static void buildIndex(Map<String, Integer> set) {
		int count = 0;
		for (Entry<String, Integer> e : set.entrySet()) {
			e.setValue(++count);	// one-based index
		}
	}
}