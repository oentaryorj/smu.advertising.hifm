package larc.recommender.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;

import larc.recommender.data.Data;
import larc.recommender.data.Instance;

public class EntityParser {
	private static final Charset charSet = Charset.forName("UTF-8");
	private static final DecimalFormat df = new DecimalFormat("0.########"); 
	
	public static Data read(String file, Data data) throws IOException {
		if (ParserUtils.countRows(file) != data.getNumRows()) {
			throw new IOException("Inconsistent number of rows!");
		}
		long start = System.currentTimeMillis() / 1000;
		BufferedReader in = Files.newBufferedReader(Paths.get(file), charSet);
		String line;
		for (int row = 0; (line = in.readLine()) != null; row++) {		// parse file
			Instance x = data.row(row);
			String[] str = line.split("[,\t ]+");
			if (str.length != 2) {
				System.out.println(line);
				throw new IOException("Invalid entity format");
			}
			x.id = str[0];
			x.weight = Double.valueOf(str[1]);	// HACK!
		}
		in.close();
		long end = System.currentTimeMillis() / 1000;
		System.out.println("Reading entities from " + file + " with " + data.getNumRows() + " rows takes " + (end - start) + " secs");
		return data;
	}
	public static void write(String file, Data data) throws IOException {
		long start = System.currentTimeMillis() / 1000;
		BufferedWriter out = Files.newBufferedWriter(Paths.get(file), charSet);
		for (Instance x : data.get()) {
			out.write(x.id + "," + df.format(x.weight) + "\n");
		}
		out.close();
		long end = System.currentTimeMillis() / 1000;
		System.out.println("Saving entities to " + file + " with " + data.getNumRows() + " rows takes " + (end - start) + " secs");
	}
}
