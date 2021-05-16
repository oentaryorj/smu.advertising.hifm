package larc.recommender.ads.unused;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import larc.recommender.ads.db.CTRMatrixExtractor;
import larc.recommender.ads.db.Range;


public class RunCTRMatrixExtractor {
	public static void main(String[] args) {
		String folder = "../data/";
		String schema = "richard_devt";
		String pageTable = "publishers_summary_oct12_ex";
		String adTable = "campaigns";
		int[] minExposes = {1000, 100, 10, 1};
		Calendar today = Calendar.getInstance();
		SimpleDateFormat date = new SimpleDateFormat("dd-MMM-yyyy");
		String MMHeader = "%%MatrixMarket matrix coordinate real general\n% Generated " + date.format(today.getTime());
		for (int thres : minExposes) {
			try {
				CTRMatrixExtractor gen = new CTRMatrixExtractor();
				gen.readSQL(schema, pageTable, adTable, new Range("2012-10-05", "2012-10-11"), thres);
				String uiFile = folder + schema + "_ui_" + thres;
				gen.filter(new Range("2012-10-05", "2012-10-10"));
				gen.writeEdge(uiFile + ".mm", " ", MMHeader);			// training data
				gen.writeSparse(uiFile + ".dat", " ");
				gen.writeEntity(uiFile + ".et");						// training entities
				gen.writeShard(uiFile + ".sh");							// training shards
				gen.filter(new Range("2012-10-11", "2012-10-11"));
				gen.writeEdge(uiFile + ".mme", " ", MMHeader);			// testing data
				gen.writeSparse(uiFile + ".date", " ");
				gen.writeEntity(uiFile + ".ete");						// testing entities
				gen.writeShard(uiFile + ".she");						// testing shards
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
