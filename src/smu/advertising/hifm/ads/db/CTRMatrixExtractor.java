package larc.recommender.ads.db;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.io.BufferedWriter;
import java.io.IOException;

public class CTRMatrixExtractor extends CTRExtractor {
	// Constructor
	public CTRMatrixExtractor() {	
		super();
	}
	// Read from database and store it internally into a HashMap
	@Override
	public Map<String, CTR> readSQL(String schema, String pageTable, String adTable, Range dayRange, int minExposes) throws Exception {
		long startTime = System.currentTimeMillis();
		Connection conn = SQLConnector.connect(schema);
		//ResultSet rs = SQLConnector.query(conn, "SELECT p.partnerid, p.cntr, p.cid," +
		//		" sum(p.sumexpose) nexpose, sum(p.sumclick) nclick FROM " + pageTable + " AS p, " + adTable + " AS a" +
		//		" WHERE a.cid = p.cid AND p.sumexpose >= " + minExposes + " GROUP BY p.partnerid, p.cntr, p.cid");
		ResultSet rs = SQLConnector.query(conn, "SELECT p.day, p.partnerid, p.cntr, p.cid, p.sumexpose, p.sumclick, p.channel" +
				" FROM " + pageTable + " AS p, " + adTable + " AS a" +
				" WHERE a.cid = p.cid AND p.day >= '" + dayRange.start + "' AND p.day <= '" + dayRange.end + "' AND p.sumexpose >= " + minExposes);
		while (rs.next()) {
			String user = rs.getString("partnerid") + "-" + rs.getString("cntr");
			String item = rs.getString("cid");
			String time = rs.getString("day");
			this.data.put(user + "," + item + "," + time, new CTR(rs.getInt("sumexpose"), rs.getInt("sumclick")));
			this.userSet.put(user, 0);
			this.itemSet.put(item, 0);
			this.timeSet.put(time, 0);
		}
		rs.close();
		conn.close();
		buildIndex(userSet);	// build indices
		buildIndex(itemSet);
		buildIndex(timeSet);
		System.out.println("Extracting CTR matrix from '" + schema + "." + pageTable + "' and '" + adTable + "' takes " + (System.currentTimeMillis() - startTime) / 1000 + " secs.");
		return this.data;
	}
	// Generic helper function to write to file
	@Override
	public void writeEdge(String file, String delim, String header) throws IOException {
		long startTime = System.currentTimeMillis();
		BufferedWriter out = Files.newBufferedWriter(Paths.get(file), charSet);
		out.write(header + "\n");	// print header
		out.write(userSet.size() + " " + itemSet.size() + " ");
		out.write(selected.size() + "\n");
		for (Entry<String, CTR> e :this.selected.entrySet()) {
			String[] pair = e.getKey().split(",");
			assert(pair.length == 2);
			out.write(userSet.get(pair[0]) + delim + itemSet.get(pair[1]) + delim + e.getValue().value() + "\n");
		}
		out.close();
		System.out.println("Saving CTR matrix to '" + file + "' takes " + (System.currentTimeMillis() - startTime) / 1000 + " secs.");
	}
	// Output tensor into sparse file (LibSVM or SVMlight-style) file, with no header information
	@Override
	public void writeSparse(String file, String delim) throws IOException {
		long startTime = System.currentTimeMillis();
		BufferedWriter out = Files.newBufferedWriter(Paths.get(file), charSet);
		for (Entry<String, CTR> e : this.selected.entrySet()) {
			String[] pair = e.getKey().split(",");
			assert(pair.length == 2);
			out.write(e.getValue().value() + delim);
			out.write(userSet.get(pair[0]) + ":" + 1 + delim);
			out.write((userSet.size() + itemSet.get(pair[1])) + ":" + 1 + "\n");
		}
		out.close();
		System.out.println("Saving CTR matrix to '" + file + "' takes " + (System.currentTimeMillis() - startTime) / 1000 + " secs.");
	}
	// Write entity information into file
	@Override
	public void writeEntity(String file) throws IOException {
		long startTime = System.currentTimeMillis();
		BufferedWriter out = Files.newBufferedWriter(Paths.get(file), charSet);
		for (Entry<String, CTR> e : this.selected.entrySet()) {
			out.write(e.getKey() + "," + e.getValue().expose + "," + e.getValue().click + "\n");
		}
		out.close();
		System.out.println("Saving entities to '" + file + "' takes " + (System.currentTimeMillis() - startTime) / 1000 + " secs.");
	}
	// Write shard information into file
	@Override
	public void writeShard(String file) throws IOException {
		long startTime = System.currentTimeMillis();
		BufferedWriter out = Files.newBufferedWriter(Paths.get(file), charSet);
		HashMap<String, Integer> shards = new HashMap<String, Integer>();
		for (Entry<String, CTR> e : this.selected.entrySet()) {
			String[] pair = e.getKey().split(",");
			assert(pair.length == 2);
			String shard = pair[0]; 
			if (!shards.containsKey(shard)) {
				shards.put(shard, shards.size());
			}
			out.write(shards.get(shard) + "\n");
		}
		out.close();
		System.out.println("Saving shards to '" + file + "' takes " + (System.currentTimeMillis() - startTime) / 1000 + " secs.");
	}
	@Override
	public Map<String, CTR> filter(Range dayRange) {
		selected.clear();
		for (Entry<String, CTR> e : this.data.entrySet()) {
			String[] triplet = e.getKey().split(",");
			assert(triplet.length == 3);
			if (dayRange.isWithin(triplet[2])) {
				String uiKey = triplet[0] + "," + triplet[1];
				CTR value = selected.get(uiKey);
				if (value == null) {
					value = new CTR(0, 0);
				}
				selected.put(uiKey, value.add(e.getValue()));
			}
		}
		return selected;
	}
}
