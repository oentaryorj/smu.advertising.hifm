package larc.recommender.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import larc.recommender.hierarchy.Hierarchy;

public class HierarchyParser {
	private static final String regex = "[,\t ]+";
	private static Charset charSet = Charset.forName("UTF-8");
	
	public static Hierarchy read(String file) throws IOException {
		long time = System.currentTimeMillis();
		BufferedReader in = Files.newBufferedReader(Paths.get(file), charSet);
		Hierarchy hier = new Hierarchy();
		int numEdges = 0;
		String line;
		while ((line = in.readLine()) != null) {		// parse file
			String[] str = line.split(regex);
			if (str.length < 1) {
				throw new IOException("Empty line");
			}
			int id = Integer.valueOf(str[0]);
			List<Integer> list = hier.get(id);
			if (list == null) {
				list = new LinkedList<Integer>();
			}
			for (int i = 1; i < str.length; i++) {
				list.add(Integer.valueOf(str[i]));
				++numEdges;
			}
			hier.put(id, list);
		}
		in.close();
		System.out.println("Reading hierarchy from " + file + " with " + numEdges + " edges takes " + (System.currentTimeMillis() - time) / 1000 + " secs");
		return hier;
	}
	public static void write(String file, Hierarchy hier) throws IOException {
		long time = System.currentTimeMillis();
		BufferedWriter out = Files.newBufferedWriter(Paths.get(file), Charset.forName("UTF-8"));
		int numEdges = 0;
		for (Entry<Integer, List<Integer>> e : hier.entrySet()) {
			StringBuilder str = new StringBuilder();
			str.append(e.getKey());
			for (Integer i : e.getValue()) {
				str.append(" ").append(i);
				++numEdges;
			}
			str.append("\n");
			out.write(str.toString());
		}
		out.close();
		System.out.println("Saving hierarchy to " + file + " with " + numEdges + " edges takes " + (System.currentTimeMillis() - time) / 1000 + " secs");
	}
}
