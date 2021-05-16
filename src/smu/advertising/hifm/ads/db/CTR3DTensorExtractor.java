package larc.recommender.ads.db;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

public class CTR3DTensorExtractor extends CTRExtractor {
	// Constructor
	public CTR3DTensorExtractor() {	
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
		out.write(userSet.size() + delim + itemSet.size()); // + delim + timeSet.size());
		out.write(delim + selected.size() + "\n");
		out.write("1" + delim + "1" + delim + timeSet.size() + delim + "0.0" + "\n");	// HACKED for GraphChi!!!
		for (Entry<String, CTR> e : this.selected.entrySet()) {
			String[] triplet = e.getKey().split(":");
			assert(triplet.length == 3);
			out.write(userSet.get(triplet[0]) + delim + itemSet.get(triplet[1]) + delim + timeSet.get(triplet[2]) + delim + e.getValue().value() + "\n");
		}
		out.close();
		System.out.println("Saving CTR tensor to '" + file + "' with " + this.selected.size() + " rows takes " + (System.currentTimeMillis() - startTime) / 1000 + " secs.");
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
			out.write(delim + (userSet.size() + itemSet.size() + timeSet.get(triplet[2]) - 1) + ":" + 1 + "\n");
		}
		out.close();
		System.out.println("Saving CTR tensor to '" + file + "' with " + this.selected.size() + " rows takes " + (System.currentTimeMillis() - startTime) / 1000 + " secs.");
	}
	// Write entity information to file
	@Override
	public void writeEntity(String file) throws IOException {
		long startTime = System.currentTimeMillis();
		BufferedWriter out = Files.newBufferedWriter(Paths.get(file), charSet);
		for (Entry<String, CTR> e : this.selected.entrySet()) {
			String[] triplet = e.getKey().split(":");
			assert(triplet.length == 3);
			out.write(e.getKey() + "," + e.getValue().expose /*+ "," + e.getValue().click*/ + "\n");
		}
		out.close();
		System.out.println("Saving entities to '" + file + "' with " + this.selected.size() + " rows takes " + (System.currentTimeMillis() - startTime) / 1000 + " secs.");
	}
	// Write shard information to file
	@Override
	public void writeShard(String file) throws IOException {
		long startTime = System.currentTimeMillis();
		BufferedWriter out = Files.newBufferedWriter(Paths.get(file), charSet);
		HashMap<String, Integer> shards = new HashMap<String, Integer>();
		for (Entry<String, CTR> e : this.selected.entrySet()) {
			String[] triplet = e.getKey().split(":");
			assert(triplet.length == 3);
			String shard = triplet[0] + "," + triplet[2]; 
			if (!shards.containsKey(shard)) {
				shards.put(shard, shards.size());
			}
			out.write(shards.get(shard) + "\n");
		}
		out.close();
		System.out.println("Saving shards to '" + file + "' with " + this.selected.size() + " rows takes " + (System.currentTimeMillis() - startTime) / 1000 + " secs.");
	}
	@Override
	public Map<String, CTR> filter(Range dayRange) {
		return filter(dayRange, dummyRange, dummyRange);
	}
	public Map<String, CTR> filter(Range dayRange, Range exclUserRange, Range exclItemRange) {
		HashSet<String> outUsers = new HashSet<String>();
		HashSet<String> outItems = new HashSet<String>();
		for (Entry<String, CTR> e : this.data.entrySet()) {
			String[] triplet = e.getKey().split(":");
			if (exclUserRange.isWithin(triplet[2])) {
				outUsers.add(triplet[0]);
			}
			if (exclItemRange.isWithin(triplet[2])) {
				outItems.add(triplet[1]);
			}
		}
		selected.clear();
		for (Entry<String, CTR> e : this.data.entrySet()) {
			String[] triplet = e.getKey().split(":");
			if (dayRange.isWithin(triplet[2]) && !outUsers.contains(triplet[0]) && !outItems.contains(triplet[1])) {
				selected.put(e.getKey(), e.getValue());
			}
		}
		return selected;
	}
	// Find the last item from the user action log (helper function)
	/*private static String findLastItem(Set<String> choices, String currTime) {
		String lastTime = "1900-01-01";
		String lastItem = "";
		String[] tokens;
		for (String choice : choices) {
			tokens = choice.split(",");
			assert(tokens.length == 2);		// tokens[0] = item, tokens[1] = time
			if (tokens[1].compareTo(lastTime) > 0 && tokens[1].compareTo(currTime) < 0) {
				lastItem = tokens[0];
				lastTime = tokens[1];
			}
		}
		return lastItem;
	}*/
}
