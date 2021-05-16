package larc.recommender.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;

import larc.recommender.data.Data;
import larc.recommender.data.Feature;
import larc.recommender.data.Instance;

public class DataParser {
	private static final Charset charSet = Charset.forName("UTF-8");
	private static final String regex = "[:,\t ]+";
	private static final DecimalFormat df = new DecimalFormat("0.########");
	
	public static Data readSparse(String file, int lineSkips, String shardFile) throws IOException {
		long start = System.currentTimeMillis() / 1000;
		BufferedReader in = Files.newBufferedReader(Paths.get(file), charSet);
		String line;
		for (int i = 0; i < lineSkips; i++) {		// skip comment lines
			line = in.readLine();
		}
		int numRows = 0, numCols = 0;
		while ((line = in.readLine()) != null) {
			String[] str = line.split(regex);
			assert(str.length % 2 == 1);
			numCols = Math.max(numCols, Integer.valueOf(str[str.length - 2]) + 1);	// zero-based index
			++numRows;
		}
		in.close();
		in = Files.newBufferedReader(Paths.get(file), Charset.forName("UTF-8"));
		Data data = new Data(numRows, numCols);
		for (int row = 0; (line = in.readLine()) != null; row++) {		// parse file
			String[] str = line.split(regex);
			assert(str.length % 2 == 1);
			for (int i = 1; i < (str.length - 1); i += 2) {
				int idx = Integer.valueOf(str[i]);	// zero-based index
				if (idx >= 0) {
					data.set(row, idx, Double.valueOf(str[i+1]));
				}
			}
			data.row(row).target = Double.valueOf(str[0]);
		}
		in.close();
		ShardParser.read(data, shardFile);
		long end = System.currentTimeMillis() / 1000;
		System.out.println("Reading from " + file + " with " + data.getNumRows() + " rows and " + data.getNumCols() + " cols takes " + (end - start) + " secs");
		return data;
	}
	public static void writeSparse(Data data, String file, String shardFile) throws IOException {
		long time = System.currentTimeMillis();
		BufferedWriter out = Files.newBufferedWriter(Paths.get(file), charSet);
		for (Instance x : data.get()) {
			out.write(Double.toString(x.target));
			for (Feature f : x) {
				out.write(" " + f.index + ":" + df.format(f.value));
			}
			out.write("\n");
		}
		out.close();
		ShardParser.write(data, shardFile);
		System.out.println("Saving data to '" + file + "' takes " + (System.currentTimeMillis() - time) / 1000 + " secs.");
	}
	/*public static Data readMM(String file, int lineSkips, String shardFile) throws IOException {
		long start = System.currentTimeMillis() / 1000;
		BufferedReader in = Files.newBufferedReader(Paths.get(file), charSet);
		int numRows = ParserUtils.countRows(file) - lineSkips - 1;
		for (int row = 0; row < lineSkips; row++) {		// skip comment lines
			in.readLine();
		}
		String line = in.readLine();					// header information
		String[] str = line.split(regex);
		int[] offsets = new int[str.length];
		offsets[0] = 0;
		for (int i = 1; i < str.length; i++) {
			offsets[i] = offsets[i-1] + Integer.valueOf(str[i-1]);
		}
		Data data = new Data(numRows, offsets[str.length - 1]);
		for(int row = 0; (line = in.readLine()) != null; row++) {		// parse file
			str = line.split("[,\t ]+");
			for (int i = 0; i < str.length - 1; i++) {
				int idx = Integer.valueOf(str[i]) - 1;	// one-based index
				if (idx >= 0) {
					data.set(row, offsets[i] + idx, 1.0);
				}
			}
			data.row(row).target = Double.valueOf(str[str.length - 1]);
		}
		in.close();
		ShardParser.read(data, shardFile);
		long end = System.currentTimeMillis() / 1000;
		System.out.println("Reading from " + file + " with " + data.getNumRows() + " rows and " + data.getNumCols() + " cols takes " + (end - start) + " secs.");
		return data;
	}*/
}
