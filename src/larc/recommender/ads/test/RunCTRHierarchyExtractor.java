package larc.recommender.ads.test;

import larc.recommender.ads.db.CTRHierarchyExtractor;
import larc.recommender.ads.db.Range;
import larc.recommender.data.Data;
import larc.recommender.parser.DataParser;
import larc.recommender.parser.EntityParser;
import larc.recommender.parser.HierarchyParser;

public class RunCTRHierarchyExtractor {
	public static void main(String[] args) {
		String folder = "../data/";
		String schema = "richard_devt";
		String pageTable = "publishers_summary_oct12_ex";
		String adTable = "campaigns";
		int[] minExposes = {1000, 100, 10};
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
		String file = folder + schema + "_uit" + fold + "_" + thres + "_hier";
		CTRHierarchyExtractor gen = new CTRHierarchyExtractor();
		gen.readSQL(schema, pageTable, adTable, trainStart, testEnd, trainStart, trainEnd, thres);
		Range trainRange = new Range(trainStart, trainEnd);
		Range testRange = new Range(testStart, testEnd);
		Range dummyRange = new Range("1970-01-01", "1970-01-01");
		Data train = gen.getTrainData(trainRange);
		Data test = gen.getPredictData(testRange);
		Data testCS1 = gen.getPredictData(testRange, trainRange, dummyRange);
		Data testCS2 = gen.getPredictData(testRange, dummyRange, trainRange);
		Data testCS3 = gen.getPredictData(testRange, trainRange, trainRange);
		HierarchyParser.write(file + ".hs", gen.getHierarchy());
		DataParser.writeSparse(train, file + ".dat", file + ".sh");
		DataParser.writeSparse(test, file + ".date", file + ".she");
		DataParser.writeSparse(testCS1, file + "_cs1.date", file + "_cs1.she");
		DataParser.writeSparse(testCS2, file + "_cs2.date", file + "_cs2.she");
		DataParser.writeSparse(testCS3, file + "_cs3.date", file + "_cs3.she");
		EntityParser.write(file + ".et", train);
		EntityParser.write(file + ".ete", test);
		EntityParser.write(file + "_cs1.ete", testCS1);
		EntityParser.write(file + "_cs2.ete", testCS2);
		EntityParser.write(file + "_cs3.ete", testCS3);
		gen.clear();	// release memory
		train.clear();	// release memory
		test.clear();	// release memory
	}
}