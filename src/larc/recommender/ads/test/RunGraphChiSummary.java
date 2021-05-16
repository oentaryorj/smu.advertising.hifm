package larc.recommender.ads.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import larc.recommender.evaluation.Ranking;
import larc.recommender.evaluation.Regression;

public class RunGraphChiSummary {
	public static void main(String[] args) {
		String folder = "../graphchi/";
		String etyFolder = "../data/";
		String baseFile = "richard_devt_uit";
		String[] algos = {"tensorals", "timesvdpp"};
		int[] minExposes = {1000, 100, 10};
		int numFolds = 10;
		try {
			BufferedWriter out = Files.newBufferedWriter(Paths.get(folder + "graphchi_results.csv"), Charset.forName("UTF-8"));
			out.write("Algorithm\tMinExpose\tFold\twRMSE\twMAE\twNLL\tR\twAUC\twMAP\twNDCG\tMRR\n");
			String delim = "[,\t ]+";
			for (String algo : algos) {
				for (int minExpose : minExposes) {
					for (int f = 1; f <= numFolds; f++) {
						String file = baseFile + f + "_" + minExpose ;
						long time = System.currentTimeMillis();
						List<Double> targets = read(folder + algo + "/" +file + ".mme", delim, 4);
						List<Double> preds = read(folder + algo + "/" + file + ".mme.predict", delim, 2);
						List<Double> exposes = read(etyFolder + file + ".she", delim, 0);
						out.write(algo + "\t" + minExpose + "\t" + f + "\t" + evaluate(targets, preds, exposes) + "\n");
						System.out.println("Processing " + file + " takes " + (System.currentTimeMillis() - time) + " ms.");
					}
				}
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static List<Double> read(String file, String delim, int lineSkips) throws IOException {
		BufferedReader in = Files.newBufferedReader(Paths.get(file), Charset.forName("UTF-8"));
		String line;
		for (int i = 0; i < lineSkips; i++) {		// skip comment lines
			line = in.readLine();
		}
		ArrayList<Double> vals = new ArrayList<Double>();
		while ((line = in.readLine()) != null) {
			String[] str = line.split(delim);
			vals.add(Double.valueOf(str[str.length - 1]));
		}
		in.close();
		return vals;
	}
	public static String evaluate(List<Double> targets, List<Double> preds, List<Double> exposes) {
		if (targets.size() != preds.size() || preds.size() != exposes.size()) {
			System.out.println(targets.size() + " " + preds.size() + " " + exposes.size());
			throw new IllegalArgumentException("Mismatched size");
		}
		int numRows = targets.size();
		Ranking rank = new Ranking();
		Regression reg = new Regression(numRows);
		for (int i = 0; i < numRows; i++) {
			int expose = (int) (double) exposes.get(i);
			int click = (int) (targets.get(i) * exposes.get(i));
			int shard = 0;	// HACK!!!
			rank.add(preds.get(i), expose, click, shard);
			reg.add(targets.get(i), preds.get(i), expose);
		}
		return reg + " " + rank;
	}
}
