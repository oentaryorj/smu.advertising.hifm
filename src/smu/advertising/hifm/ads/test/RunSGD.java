package larc.recommender.ads.test;

import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import larc.recommender.base.Config;
import larc.recommender.data.Data;
import larc.recommender.learn.SGD;
import larc.recommender.loss.LogisticLoss;
import larc.recommender.loss.Loss;
import larc.recommender.loss.SquareLoss;
import larc.recommender.parser.DataParser;
import larc.recommender.parser.EntityParser;
import larc.recommender.regularization.L2;
import larc.recommender.regularization.Regularization;

public class RunSGD extends BaseRun {
	public static void main(String[] args) {
		Loss[] losses = {new SquareLoss(0.0, 1.0), new LogisticLoss()};
		String[] types = {"rank"};
		int[] minExposes = {1000, 100, 10};
		int numFolds = 10;
		Config params = new Config();
		for (String type : types) {
			for (Loss loss : losses) {
				try {
					BufferedWriter out = Files.newBufferedWriter(Paths.get("../data/SGD_" + loss + "_" + type + ".out"), Charset.forName("UTF-8"));
					for (int minExpose : minExposes) {
						for (int i = 1; i <= numFolds; i++) {			// no hierarchy
							String baseFile = "../data/richard_devt_uit" + i + "_" + minExpose;
							Data train = DataParser.readSparse(baseFile + ".dat", 0, baseFile + ".sh").setName(baseFile);
							Data test = DataParser.readSparse(baseFile + ".date", 0, baseFile + ".she").setName(baseFile);
							Data testCS1 = DataParser.readSparse(baseFile + "_cs1.date", 0, baseFile + "_cs1.she").setName(baseFile + "_cs1");
							Data testCS2 = DataParser.readSparse(baseFile + "_cs2.date", 0, baseFile + "_cs2.she").setName(baseFile + "_cs2");
							Data testCS3 = DataParser.readSparse(baseFile + "_cs3.date", 0, baseFile + "_cs3.she").setName(baseFile + "_cs3");
							train = EntityParser.read(baseFile + ".et", train);
							test = EntityParser.read(baseFile + ".ete", test);
							testCS1 = EntityParser.read(baseFile + "_cs1.ete", testCS1);
							testCS2 = EntityParser.read(baseFile + "_cs2.ete", testCS2);
							testCS3 = EntityParser.read(baseFile + "_cs3.ete", testCS3);
							out.write("Data " + baseFile + "\n");
							Regularization reg = new L2();
							SGD fm = new SGD(Math.max(train.getNumCols(), test.getNumCols()), params, loss, reg);
							fm.learn(train, test);
							BufferedWriter log = Files.newBufferedWriter(Paths.get(baseFile + "_SGD_" + loss + "_" + type + ".log"), Charset.forName("UTF-8"));
							log.write(fm.getTrace());
							log.close();
							String trainStr = evaluate(train, fm.predict(train));
							String testStr = evaluate(test, fm.predict(test));
							String testCS1Str = evaluate(testCS1, fm.predict(testCS1));
							String testCS2Str = evaluate(testCS2, fm.predict(testCS2));
							String testCS3Str = evaluate(testCS3, fm.predict(testCS3));
							System.out.println("MinExpose: " + minExpose + " Regularization: " + reg + " Type: " + type);
							System.out.println("- Train: " + trainStr + "\n- Test: " + testStr);
							System.out.println("- Cold_test1: " + testCS1Str + "\n- Cold_test2: " + testCS2Str +  "\n- Cold_test3: " + testCS3Str);
							out.write("MinExpose: " + minExpose + " Regularization: " + reg + " Type: " + type + "\n");
							out.write("- Train: " + trainStr + "\n- Test: " + testStr + "\n");
							out.write("- Cold_test1: " + testCS1Str + "\n- Cold_test2: " + testCS2Str +  "\n- Cold_test3: " + testCS3Str + "\n");
							train.clear();	// release memory
							test.clear();	// release memory
							testCS1.clear();	// release memory
							testCS2.clear();	// release memory
							testCS3.clear();	// release memory
						}
					}
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
