package larc.recommender.ads.db;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public class CTR4DTensorExtractor extends CTR3DTensorExtractor {
	private Map<String, Set<String>> userChoices = new HashMap<String, Set<String>>();
	
	// Constructor
	public CTR4DTensorExtractor() {	
		super();
	}
	// Read from database and store it internally into a HashMap
	@Override
	public Map<String, CTR> readSQL(String schema, String pageTable, String adTable, Range dayRange, int minExposes) throws Exception {
		long startTime = System.currentTimeMillis();
		Connection conn = SQLConnector.connect(schema);
		ResultSet rs = SQLConnector.query(conn, "SELECT p.day, p.partnerid, p.cntr, p.cid, p.sumexpose, p.sumclick, p.channel" +
				" FROM " + pageTable + " AS p, " + adTable + " AS a" +
				" WHERE a.cid = p.cid AND p.day >= '" + dayRange.start + "' AND p.day <= '" + dayRange.end + "' AND p.sumexpose >= " + minExposes);
		while (rs.next()) {
			String user = rs.getString("partnerid") + "-" + rs.getString("cntr");
			String item = rs.getString("cid");
			String time = rs.getString("day");
			this.data.put(user + ":" + item + ":" + time, new CTR(rs.getLong("sumexpose"), rs.getLong("sumclick")));
			this.userSet.put(user, 0);
			this.itemSet.put(item, 0);
			this.timeSet.put(time, 0);
			Set<String> choices = userChoices.get(user);
			if (choices == null) {
				choices = new TreeSet<String>();
			}
			choices.add(item);
			userChoices.put(user, choices);
		}
		rs.close();
		conn.close();
		buildIndex(userSet);	// build indices
		buildIndex(itemSet);
		buildIndex(timeSet);
		System.out.println("Extracting CTR tensor from '" + schema + "." + pageTable + "' and '" + adTable + "' takes " + (System.currentTimeMillis() - startTime) / 1000 + " secs.");
		return this.data;
	}
	// Generic helper function to write to file
	@Override
	public void writeEdge(String file, String delim, String header) throws IOException {
		long startTime = System.currentTimeMillis();
		BufferedWriter out = Files.newBufferedWriter(Paths.get(file), charSet);
		out.write(header + "\n");	// print header
		out.write(userSet.size() + delim + itemSet.size() + delim + timeSet.size());
		out.write(delim + selected.size() + "\n");
		for (Entry<String, CTR> e : this.selected.entrySet()) {
			String[] triplet = e.getKey().split(":");
			assert(triplet.length == 3);
			out.write(userSet.get(triplet[0]) + delim);
			out.write(itemSet.get(triplet[1]) + delim);
			out.write(timeSet.get(triplet[2]) + delim);
			String items = "{";
			for (String item : userChoices.get(triplet[0])) {
				items += itemSet.get(item) + ",";
			}
			out.write(items.substring(0, items.lastIndexOf(",")) + "}" + delim);
			out.write(e.getValue().value() + "\n");
		}
		out.close();
		System.out.println("Saving CTR tensor to '" + file + "' takes " + (System.currentTimeMillis() - startTime) / 1000 + " secs.");
	}
	// Output tensor into sparse file (LibSVM or SVMlight-style) file, with no header information
	@Override
	public void writeSparse(String file, String delim) throws IOException {
		long startTime = System.currentTimeMillis();
		BufferedWriter out = Files.newBufferedWriter(Paths.get(file), charSet);
		for (Entry<String, CTR> e : this.selected.entrySet()) {
			String[] triplet = e.getKey().split(":");
			assert(triplet.length == 3);
			out.write(Double.toString(e.getValue().value()));
			out.write(delim + (userSet.get(triplet[0]) - 1) + ":" + 1);
			out.write(delim + (userSet.size() + itemSet.get(triplet[1]) - 1) + ":" + 1);
			out.write(delim + (userSet.size() + itemSet.size() + timeSet.get(triplet[2]) - 1) + ":" + 1);
			Set<String> choices = userChoices.get(triplet[0]);
			int numChoices = choices.size();
			for (String item : choices) {
				out.write(delim + (userSet.size() + itemSet.size() + timeSet.size() + itemSet.get(item)) + ":" + (1.0 / numChoices));
			}
			out.write("\n");
		}
		out.close();
		System.out.println("Saving CTR tensor to '" + file + "' takes " + (System.currentTimeMillis() - startTime) / 1000 + " secs.");
	}
}
