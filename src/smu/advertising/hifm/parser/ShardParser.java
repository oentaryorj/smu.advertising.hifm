package larc.recommender.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import larc.recommender.data.Data;
import larc.recommender.data.Instance;



public class ShardParser {
	private static final Charset charSet = Charset.forName("UTF-8");
	
	public static void read(Data data, String file) throws IOException {
		if (data.getNumRows() != ParserUtils.countRows(file)) {
			throw new IOException("Inconsistent number of rows! Data: " + data.getNumRows() + " vs. file: " + ParserUtils.countRows(file));
		}
		BufferedReader in = Files.newBufferedReader(Paths.get(file), charSet);
		String line;
		for (int row = 0; (line = in.readLine()) != null; row++) {		// parse file
			data.row(row).shard = Integer.valueOf(line);
		}
		in.close();
	}
	public static void write(Data data, String file) throws IOException {
		BufferedWriter out = Files.newBufferedWriter(Paths.get(file), charSet);
		for (Instance x : data.get()) {
			out.write(x.shard + "\n");
		}
		out.close();
	}
}
