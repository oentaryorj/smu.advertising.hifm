package larc.recommender.ads.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;

public class RunEvalSummary {
	public static void main(String[] args) {
		String folder = "../data/";
		String file = "benchmark_results.csv";
		String outCSVFile = "benchmark_summary.csv";
		String outTexFile = "benchmark_summary.tex";
		String[] algorithms = { 
			"SGD_square_rank", "SGD_square_rank_hybrid", "WSGD_square_rank", "WSGD_square_rank_hybrid",
			"SGD_logistic_rank", "SGD_logistic_rank_hybrid", "WSGD_logistic_rank", "WSGD_logistic_rank_hybrid",
			"CD_square_cyclic", "CD_square_cyclic_hybrid", "WCD_square_cyclic", "WCD_square_cyclic_hybrid",
			"CD_square_stochastic", "CD_square_stochastic_hybrid", "WCD_square_stochastic", "WCD_square_stochastic_hybrid"
		};
		String[] cases = {"- Test", "- Cold_test1", "- Cold_test2", "- Cold_test3"};
		int minExposes[] = {10, 100, 1000};
		int numFolds = 10;
		int count = 0;
		try {
			for (int minExpose : minExposes) {
				for (String c : cases) {
					for (int i = 0; i < algorithms.length; i++) {
						String algo = algorithms[i];
						BufferedWriter out = new BufferedWriter(new FileWriter(folder + file, count++ != 0));
						out.write("Algo: " + algo + ((i % 4 == 0)? " [Baseline]\n" : "\n"));
						out.write("-------------------------------------\n");
						out.close();
						extract(folder + algo + ".out", folder + file, c, numFolds, minExpose);
					}
				}
			} 
			summarize(folder + file, folder + outCSVFile, "\t", "", "\t");
			summarize(folder + file, folder + outTexFile, " & ", "$", " \\pm ");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void extract(String inFile, String outFile, String testCase, int numFolds, int minExpose) throws IOException {
		long time = System.currentTimeMillis();
		BufferedReader in = Files.newBufferedReader(Paths.get(inFile), Charset.forName("UTF-8"));
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile, true));
		out.write(testCase + "\n");
		out.write("MinExpose\tRMSE\tLL\tAUC\tNDCG\n");
		String line;
		int count = 0;
		boolean flag = false;
		while ((line = in.readLine()) != null) {
			if (line.startsWith("MinExpose")) {
				int expose = Integer.valueOf(line.split("[: ]+")[1]);
				if (flag = (expose == minExpose)) {
					out.write(line.split("[: ]+")[1] + "\t");
				}
			}
			if (flag && line.startsWith(testCase)) {
				for (String s : line.split(" ")) {
					if (s.startsWith("RMSE")) {
						out.write(s.split("=")[1] + "\t");
					} else if (s.startsWith("LL")) {
						out.write(s.split("=")[1] + "\t");
					} else if (s.startsWith("AUC")) {
						out.write(s.split("=")[1] + "\t");
					} else if (s.startsWith("NDCG")) {
						out.write(s.split("=")[1] + "\t");
					}
				}
				out.write("\n");
				if (++count % numFolds == 0) {
					out.write("\n");
				}
			}
		}
		in.close();
		out.close();
		System.out.println("Extracting from " + inFile + " takes " + (System.currentTimeMillis() - time) / 1000 + " secs");
	}
	public static void summarize(String inFile, String outFile, String delim, String presuf, String stdsep) throws IOException {
		DecimalFormat df = new DecimalFormat("0.########");
		long time = System.currentTimeMillis();
		BufferedReader in = Files.newBufferedReader(Paths.get(inFile), Charset.forName("UTF-8"));
		BufferedWriter out = Files.newBufferedWriter(Paths.get(outFile), Charset.forName("UTF-8"));
		DescriptiveStatistics RMSE = new DescriptiveStatistics();
		DescriptiveStatistics LL = new DescriptiveStatistics();
		DescriptiveStatistics AUC = new DescriptiveStatistics();
		DescriptiveStatistics NDCG = new DescriptiveStatistics();
		DescriptiveStatistics baseRMSE = RMSE;
		DescriptiveStatistics baseLL = LL; 
		DescriptiveStatistics baseAUC = AUC;
		//DescriptiveStatistics baseNDCG = NDCG;
		String line;
		int count = 0;
		int minExpose = 0;
		String testCase = "";
		boolean baseline = false;
		//out.write("Algo\tCase\tMinExpose\tRMSE_avg \\pm RMSE_std\tLL_avg \\pm LL_std\tAUC_avg \\pm AUV_std\tNDCG_avg \\pm NDCG_std\tRMSE_sig\tLL_sig\tAUC_sig\tNDCG_sig\n");
		out.write("Algo\tCase\tMinExpose\tRMSE_avg \\pm RMSE_std\tLL_avg \\pm LL_std\tAUC_avg \\pm AUC_std\tRMSE_sig\tLL_sig\tAUC_sig\n");
		while ((line = in.readLine()) != null) {
			if (line.startsWith("Algo")) {
				out.write(line.split("[: ]+")[1] + "\t");
				baseline = line.contains("Baseline");
			}
			if (line.startsWith("- ")) {
				testCase = line.split("[ ]+")[1];
			}
			String[] str = line.split("[ \t]+");
			if (isInteger(str[0])) {
				minExpose = Integer.valueOf(str[0]);
				RMSE.addValue(Double.valueOf(str[1]));
				LL.addValue(Double.valueOf(str[2]));
				AUC.addValue(Double.valueOf(str[3]));
				NDCG.addValue(Double.valueOf(str[4]));
				++count;
			} else {
				if (count > 0) {
					out.write(testCase + delim + minExpose + delim + 
							presuf + df.format(RMSE.getMean()) + stdsep + df.format(RMSE.getStandardDeviation()) + presuf + delim + 
							presuf + df.format(LL.getMean()) + stdsep + df.format(LL.getStandardDeviation()) + presuf + delim + 
							presuf + df.format(AUC.getMean()) + stdsep + df.format(AUC.getStandardDeviation())); // presuf + delim + 
							//presuf + df.format(NDCG.getMean()) + stdsep + df.format(NDCG.getStandardDeviation()));
					if (baseline) {
						baseRMSE = RMSE;
						baseLL = LL; 
						baseAUC = AUC;
						//baseNDCG = NDCG;
					} else {
						WilcoxonSignedRankTest wt = new WilcoxonSignedRankTest();
						out.write("\t");
						out.write((wt.wilcoxonSignedRankTest(baseRMSE.getValues(), RMSE.getValues(), true) < 0.01) + "\t" +
								  (wt.wilcoxonSignedRankTest(baseLL.getValues(), LL.getValues(), true) < 0.01) + "\t" +
								  (wt.wilcoxonSignedRankTest(baseAUC.getValues(), AUC.getValues(), true) < 0.01)); // + "\t" +
								  //(wt.wilcoxonSignedRankTest(baseNDCG.getValues(), NDCG.getValues(), true) < 0.01));
					}
					out.write("\n");
				}
				RMSE = new DescriptiveStatistics();
				LL = new DescriptiveStatistics();
				AUC = new DescriptiveStatistics();
				NDCG = new DescriptiveStatistics();
				count = 0;
			}
		}
		in.close();
		out.close();
		System.out.println("Summarizing from " + inFile + " takes " + (System.currentTimeMillis() - time) / 1000 + " secs");
	}
	private static boolean isInteger(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isDigit(s.charAt(i))) {
				return false;
			}
		}
		return (s.length() > 0);
	}
}
