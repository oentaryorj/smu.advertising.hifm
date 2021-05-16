package larc.recommender.ads.test;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import larc.recommender.ads.db.CTR3DTensorExtractor;
import larc.recommender.ads.db.Range;

public class RunCTRTensorExtractor {
	public static void main(String[] args) {
		String folder = "../data/";
		String schema = "richard_devt";
		String pageTable = "publishers_summary_oct12_ex";
		String adTable = "campaigns";
		int[] minExposes = {1000}; //, 100, 10};
		try {
			for (int thres : minExposes) {
				//run(folder, schema, 1, thres, pageTable, adTable, "2012-10-05", "2012-10-08", "2012-10-09", "2012-10-11");
				//run(folder, schema, 2, thres, pageTable, adTable, "2012-10-10", "2012-10-13", "2012-10-14", "2012-10-16");
				//run(folder, schema, 3, thres, pageTable, adTable, "2012-10-15", "2012-10-18", "2012-10-19", "2012-10-21");
				//run(folder, schema, 4, thres, pageTable, adTable, "2012-10-20", "2012-10-23", "2012-10-24", "2012-10-26");
				//run(folder, schema, 5, thres, pageTable, adTable, "2012-10-25", "2012-10-28", "2012-10-29", "2012-10-31");
				run(folder, schema, 1, thres, pageTable, adTable, "2012-10-05", "2012-10-11", "2012-10-12", "2012-10-13");
				run(folder, schema, 2, thres, pageTable, adTable, "2012-10-07", "2012-10-13", "2012-10-14", "2012-10-15");
				run(folder, schema, 3, thres, pageTable, adTable, "2012-10-09", "2012-10-15", "2012-10-16", "2012-10-17");
				run(folder, schema, 4, thres, pageTable, adTable, "2012-10-11", "2012-10-17", "2012-10-18", "2012-10-19");
				run(folder, schema, 5, thres, pageTable, adTable, "2012-10-13", "2012-10-19", "2012-10-20", "2012-10-21");
				run(folder, schema, 6, thres, pageTable, adTable, "2012-10-15", "2012-10-21", "2012-10-22", "2012-10-23");
				run(folder, schema, 7, thres, pageTable, adTable, "2012-10-17", "2012-10-23", "2012-10-24", "2012-10-25");
				run(folder, schema, 8, thres, pageTable, adTable, "2012-10-19", "2012-10-25", "2012-10-26", "2012-10-27");
				run(folder, schema, 9, thres, pageTable, adTable, "2012-10-21", "2012-10-27", "2012-10-28", "2012-10-29");
				run(folder, schema, 10, thres, pageTable, adTable, "2012-10-23", "2012-10-29", "2012-10-30", "2012-10-31");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private static void run(String folder, String schema, int fold, int thres, String pageTable, String adTable, 
						    String trainStart, String trainEnd, String testStart, String testEnd) throws Exception {
		String uitFile = folder + schema + "_uit" + fold + "_" + thres;
		CTR3DTensorExtractor gen = new CTR3DTensorExtractor();
		gen.readSQL(schema, pageTable, adTable, new Range(trainStart, testEnd), thres);
		//gen.setImplicit(false);
		Range trainRange = new Range(trainStart, trainEnd);
		Range testRange = new Range(testStart, testEnd);
		Range dummyRange = new Range("1970-01-01", "1970-01-01");
		Calendar today = Calendar.getInstance();
		SimpleDateFormat date = new SimpleDateFormat("dd-MMM-yyyy");
		String MMHeader = "%%MatrixMarket matrix coordinate real general\n% Generated " + date.format(today.getTime());
		gen.filter(trainRange);
		gen.writeEdge(uitFile + ".mm", " ", MMHeader);			// training data
		gen.writeSparse(uitFile + ".dat", " ");
		gen.writeEntity(uitFile + ".et");						// training entities
		gen.writeShard(uitFile + ".sh");						// training shards
		gen.filter(testRange);
		gen.writeEdge(uitFile + ".mme", " ", MMHeader);			// testing data
		gen.writeSparse(uitFile + ".date", " ");
		gen.writeEntity(uitFile + ".ete");						// testing entities
		gen.writeShard(uitFile + ".she");						// testing shards
		gen.filter(testRange, trainRange, dummyRange);
		gen.writeEdge(uitFile + "_cs1.mme", " ", MMHeader);			// testing data
		gen.writeSparse(uitFile + "_cs1.date", " ");
		gen.writeEntity(uitFile + "_cs1.ete");						// testing entities
		gen.writeShard(uitFile + "_cs1.she");						// testing shards
		gen.filter(testRange, dummyRange, trainRange);
		gen.writeEdge(uitFile + "_cs2.mme", " ", MMHeader);			// testing data
		gen.writeSparse(uitFile + "_cs2.date", " ");
		gen.writeEntity(uitFile + "_cs2.ete");						// testing entities
		gen.writeShard(uitFile + "_cs2.she");						// testing shards
		gen.filter(testRange, trainRange, trainRange);
		gen.writeEdge(uitFile + "_cs3.mme", " ", MMHeader);			// testing data
		gen.writeSparse(uitFile + "_cs3.date", " ");
		gen.writeEntity(uitFile + "_cs3.ete");						// testing entities
		gen.writeShard(uitFile + "_cs3.she");						// testing shards
	}
}
