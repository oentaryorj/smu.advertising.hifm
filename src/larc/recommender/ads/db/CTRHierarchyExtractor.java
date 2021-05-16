package larc.recommender.ads.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import larc.recommender.ads.db.SQLConnector;
import larc.recommender.data.Data;
import larc.recommender.data.Feature;
import larc.recommender.data.Instance;
import larc.recommender.hierarchy.Hierarchy;
import larc.recommender.hierarchy.Node;

public class CTRHierarchyExtractor {
	private final Map<String, Node> leafMap = new TreeMap<String, Node>();
	private final Map<String, Node> parentMap = new TreeMap<String, Node>();
	private final Map<Integer, Set<Integer>> hier = new HashMap<Integer, Set<Integer>>();
	private final Map<String, Integer> pages = new TreeMap<String, Integer>();
	private final Map<String, Integer> ads = new TreeMap<String, Integer>();
	private final Map<String, Integer> days = new TreeMap<String, Integer>();
	private final Map<String, Integer> publishers = new TreeMap<String, Integer>();
	private final Map<String, Integer> countries = new TreeMap<String, Integer>();
	private final Map<String, Integer> channels = new TreeMap<String, Integer>();
	private final Map<String, Integer> advertisers = new TreeMap<String, Integer>();
	private final Map<String, Integer> banners = new TreeMap<String, Integer>();
	private static final Range dummyRange = new Range("1970-01-01", "1970-01-01");
	private int numCols = 0;
	
	public CTRHierarchyExtractor() {
	}
	public void readSQL(String schema, String pageTable, String adTable, String startDay, String endDay, String aggStartDay, String aggEndDay, int minExposes) throws Exception {
		long startTime = System.currentTimeMillis();
		Connection conn = SQLConnector.connect(schema);
		ResultSet rs = SQLConnector.query(conn, "SELECT p.day, p.partnerid AS publisher, p.cntr, p.cid, p.sumexpose, p.sumclick, p.channel," +
							" a.partnerid AS advertiser, a.bannertype FROM " + pageTable + " AS p, " + adTable + " AS a" +
							" WHERE a.cid = p.cid AND p.day >= '" + startDay + "' AND p.day <= '" + endDay + "' AND p.sumexpose >= " + minExposes);
		while (rs.next()) {
			String day = rs.getString("day");
			String publisher = rs.getString("publisher");
			String country = rs.getString("cntr");
			String page = publisher + "-" + country;
			String channel = rs.getString("channel");
			String ad = rs.getString("cid");
			String advertiser = "av-" + rs.getString("advertiser");	// HACK: cid can sometimes be the same as advertiser
			String banner = rs.getString("bannertype");
			pages.put(page, 0);
			ads.put(ad, 0);
			days.put(day, 0);
			publishers.put(publisher, 0);
			countries.put(country, 0);
			channels.put(channel, 0);
			advertisers.put(advertiser, 0);
			banners.put(banner, 0);
			long expose = rs.getLong("sumexpose");
			long click = rs.getLong("sumclick");
		
			add(page + ":" + ad + ":" + day, expose, click, leafMap);
			if (day.compareTo(aggStartDay) < 0 || day.compareTo(aggEndDay) > 0) {
				continue;
			}
			add(page + ":" + advertiser + ":" + day, expose, click, parentMap);
			//add(page + ":" + banner + ":" + day, expose, click, parentMap);
			add(publisher + ":" + ad + ":" + day, expose, click, parentMap);
			add(publisher + ":" + advertiser + ":" + day, expose, click, parentMap);
			//add(publisher + ":" + banner + ":" + day, expose, click, parentMap);
			//add(country + ":" + ad + ":" + day, expose, click, parentMap);
			//add(country + ":" + advertiser + ":" + day, expose, click, parentMap);
			//add(country + ":" + banner + ":" + day, expose, click, parentMap);
			add(channel + ":" + ad + ":" + day, expose, click, parentMap);
			add(channel + ":" + advertiser + ":" + day, expose, click, parentMap);
			//add(channel + ":" + banner + ":" + day, expose, click, parentMap);
			add("_:" + ad + ":" + day, expose, click, parentMap);			// page root
			add("_:" + advertiser + ":" + day, expose, click, parentMap);	// page root
			//add("_:" + banner + ":" + day, expose, click, parentMap);		// page root
			add(page + ":_:" + day, expose, click, parentMap);				// ad root
			add(publisher + ":_:" + day, expose, click, parentMap);			// ad root
			//add(country + ":_:" + day, expose, click, parentMap);			// ad root
			add(channel + ":_:" + day, expose, click, parentMap);			// ad root
			
			add(page + ":" + advertiser + ":" + "_", expose, click, parentMap); 	// add instance to day root
			//add(page + ":" + banner + ":" + "_", expose, click, parentMap);
			add(publisher + ":" + ad + ":" + "_", expose, click, parentMap);
			add(publisher + ":" + advertiser + ":" + "_", expose, click, parentMap);
			//add(publisher + ":" + banner + ":" + "_", expose, click, parentMap);
			//add(country + ":" + ad + ":" + "_", expose, click, parentMap);
			//add(country + ":" + advertiser + ":" + "_", expose, click, parentMap);
			//add(country + ":" + banner + ":" + "_", expose, click, parentMap);
			add(channel + ":" + ad + ":" + "_", expose, click, parentMap);
			add(channel + ":" + advertiser + ":" + "_", expose, click, parentMap);
			//add(channel + ":" + banner + ":" + "_", expose, click, parentMap);
			add("_:" + ad + ":" + "_", expose, click, parentMap);			// page root
			add("_:" + advertiser + ":" + "_", expose, click, parentMap);	// page root
			//add("_:" + banner + ":" + "_", expose, click, parentMap);		// page root
			add(page + ":_:" + "_", expose, click, parentMap);				// ad root
			add(publisher + ":_:" + "_", expose, click, parentMap);			// ad root
			//add(country + ":_:" + "_", expose, click, parentMap);			// ad root
			add(channel + ":_:" + "_", expose, click, parentMap);			// ad root
		}
		rs.close();
		conn.close();
		int colOffset = 0;
		colOffset = buildIndex(pages, colOffset);		// build indices
		colOffset = buildIndex(ads, colOffset);
		colOffset = buildIndex(days, colOffset);
		colOffset = buildIndex(publishers, colOffset);
		//colOffset = buildIndex(countries, colOffset);
		colOffset = buildIndex(channels, colOffset);
		colOffset = buildIndex(advertisers, colOffset);
		//colOffset = buildIndex(banners, colOffset);
		this.numCols = colOffset + 3;		// Save total number columns (additional 3 columns for root nodes)
		conn = SQLConnector.connect(schema);
		rs = SQLConnector.query(conn, "SELECT p.day, p.partnerid AS publisher, p.cntr, p.cid, p.sumexpose, p.sumclick, p.channel," +
							" a.partnerid AS advertiser, a.bannertype FROM " + pageTable + " AS p, " + adTable + " AS a" +
							" WHERE a.cid = p.cid AND p.day >= '" + startDay + "' AND p.day <= '" + endDay + "' AND p.sumexpose >= " + minExposes);
		while (rs.next()) {
			String day = rs.getString("day");
			String publisher = rs.getString("publisher");
			String country = rs.getString("cntr");
			String page = publisher + "-" + country;
			String channel = rs.getString("channel");
			String ad = rs.getString("cid");
			String advertiser = "av-" + rs.getString("advertiser");	// HACK: cid can sometimes be the same as advertiser id
			//String banner = rs.getString("bannertype");
			
			addHierarchy(pages.get(page), publishers.get(publisher));
			//addHierarchy(pages.get(page), countries.get(country));
			addHierarchy(publishers.get(publisher), channels.get(channel));
			addHierarchy(ads.get(ad), advertisers.get(advertiser));
			//addHierarchy(advertisers.get(advertiser), banners.get(banner));
			addHierarchy(channels.get(channel), numCols - 3);		// add link to page root
			addHierarchy(advertisers.get(advertiser), numCols - 2);			// add link to ad root
			addHierarchy(days.get(day), numCols - 1);				// add link to day root
		}
		rs.close();
		conn.close();
		System.out.println("Extracting hierarchy from '" + schema + "." + pageTable + "' and '" + adTable + 
						   "' takes " + (System.currentTimeMillis() - startTime) / 1000 + " secs.");
	}
	// Clear the data structures
	public void clear() {
		for (Entry<Integer, Set<Integer>> e : hier.entrySet()) {
			e.getValue().clear();
		}
		this.leafMap.clear();
		this.parentMap.clear();
		this.hier.clear();
		this.pages.clear();
		this.publishers.clear();
		this.countries.clear();
		this.channels.clear();
		this.ads.clear();
		this.advertisers.clear();
		this.banners.clear();
	}
	// Extract train data
	public Data getTrainData(Range dayRange) throws IOException {
		LinkedList<Instance> instances = new LinkedList<Instance>();
		double leafSum = appendLeaves(instances, dayRange);
		int numLeaves = instances.size();
		double parentSum = appendParents(instances);
		Data data = new Data(instances.size(), this.numCols);
		int row = 0;
		for (Instance x : instances) {
			for (Feature f : x) {
				data.set(row, f.index, f.value);
			}
			Instance xd = data.row(row);
			xd.weight = x.weight;
			if (row >= numLeaves) {
				xd.weight *= 0.5 * (leafSum / parentSum);	// IMPORTANT: Need  to rescale parents' weights
			}
			xd.shard = x.shard;
			xd.id = x.id;
			xd.target = x.target;
			row++;
		}
		return data;
	}
	// Extract prediction data
	public Data getPredictData(Range dayRange) throws IOException {
		return getPredictData(dayRange, dummyRange, dummyRange);
	}
	public Data getPredictData(Range dayRange, Range exclUserRange, Range exclItemRange) throws IOException {
		LinkedList<Instance> instances = new LinkedList<Instance>();
		appendLeaves(instances, dayRange, exclUserRange, exclItemRange);
		Data data = new Data(instances.size(), this.numCols);
		int row = 0;
		for (Instance x : instances) {
			//augmentParents(x);			// augment parent features
			for (Feature f : x) {
				data.set(row, f.index, f.value);
			}
			Instance xd = data.row(row);
			xd.weight = x.weight;					
			xd.shard = x.shard;
			xd.id = x.id;
			xd.target = x.target;
			row++;
		}
		return data;
	}
	// Output the hierarchy
	public Hierarchy getHierarchy() throws IOException {
		Hierarchy hierarchy = new Hierarchy();
		for (Entry<Integer, Set<Integer>> e : hier.entrySet()) {
			hierarchy.put(e.getKey(), new LinkedList<Integer>(e.getValue()));
		}
		return hierarchy;
	}
	// Append leaf instances
	private double appendLeaves(List<Instance> instances, Range dayRange) throws IOException {
		return appendLeaves(instances, dayRange, dummyRange, dummyRange);
	}
	// Append leaf instances
	private double appendLeaves(List<Instance> instances, Range dayRange, Range exclUserRange, Range exclItemRange) throws IOException {
		HashSet<String> outUsers = new HashSet<String>();
		HashSet<String> outItems = new HashSet<String>();
		for (Entry<String, Node> e : leafMap.entrySet()) {		// extract leaf data first
			Node node = e.getValue();
			String[] ids = node.id.split(":");
			if (ids.length != 3) {
				throw new IOException("Not a triplet");
			}
			if (ids[0].equals("_") || ids[1].equals("_") || ids[2].equals("_")) {
				throw new IOException("Not a leaf node");
			}
			if (exclUserRange.isWithin(ids[2])) {
				outUsers.add(ids[0]);
			}
			if (exclItemRange.isWithin(ids[2])) {
				outItems.add(ids[1]);
			}
		}
		HashMap<String, Integer> shards = new HashMap<String, Integer>();
		double sumWeight = 0.0;
		for (Entry<String, Node> e : leafMap.entrySet()) {		// extract leaf data first
			Node node = e.getValue();
			String[] ids = node.id.split(":");
			if (ids.length != 3) {
				throw new IOException("Not a triplet");
			}
			if (ids[0].equals("_") || ids[1].equals("_") || ids[2].equals("_")) {
				throw new IOException("Not a leaf node");
			}
			if (dayRange.isWithin(ids[2]) && !outUsers.contains(ids[0]) && !outItems.contains(ids[1])) {
				Instance x = new Instance();
				x.add(new Feature(pages.get(ids[0]), 1.0));
				x.add(new Feature(ads.get(ids[1]), 1.0));
				x.add(new Feature(days.get(ids[2]), 1.0));
				String shard = ids[0] + "," + ids[2]; 
				if (!shards.containsKey(shard)) {
					shards.put(shard, shards.size());
				}
				x.shard = shards.get(shard);
				x.target = node.value();
				x.id = node.id;
				x.weight = (double) node.expose;
				instances.add(x);
				sumWeight += x.weight;
			}
		}
		return sumWeight;
	}
	// Append parent (non-leaf) instances
	private double appendParents(List<Instance> instances) throws IOException {
		TreeMap<Integer, Double> sorted = new TreeMap<Integer, Double>();
		double sumWeight = 0.0;
		for (Entry<String, Node> e : parentMap.entrySet()) { 
			Node node = e.getValue();
			String[] ids = node.id.split(":");
			if (ids.length != 3) {
				throw new IOException("Not a triplet");
			}
			sorted.clear();	// reset
			if (pages.containsKey(ids[0])) {	// page hierarchy
				sorted.put(pages.get(ids[0]), 1.0);
			} else if (publishers.containsKey(ids[0])) {
				sorted.put(publishers.get(ids[0]), 1.0);
			} else if (countries.containsKey(ids[0])) {
				sorted.put(countries.get(ids[0]), 1.0);
			} else if (channels.containsKey(ids[0])) {
				sorted.put(channels.get(ids[0]), 1.0);
			} else {
				sorted.put(this.numCols - 2, 1.0);		// page root node
			}
			if (ads.containsKey(ids[1])) {		// ad hierarchy
				sorted.put(ads.get(ids[1]), 1.0);
			} else if (advertisers.containsKey(ids[1])) {
				sorted.put(advertisers.get(ids[1]), 1.0);
			} else if (banners.containsKey(ids[1])) {
				sorted.put(banners.get(ids[1]), 1.0);
			} else {
				sorted.put(this.numCols - 1, 1.0);		// ad root node
			}
			if (ads.containsKey(ids[2])) {
				sorted.put(days.get(ids[2]), 1.0);
			}
			Instance x = new Instance();
			for (Entry<Integer, Double> s : sorted.entrySet()) {
				x.add(new Feature(s.getKey(), s.getValue()));
			}
			x.shard = -1;		// HACK FOR NOW!!
			x.target = node.value();
			x.id = node.id;
			x.weight = (double) node.expose;
			instances.add(x);
			sumWeight += x.weight;
		}
		return sumWeight;
	}
	// Helper function to insert node into edges
	private static Node add(String id, long expose, long click, Map<String, Node> map) {
		Node node = map.get(id);
		if (node == null) {
			node = new Node(id, expose, click);
		} else {
			node.add(expose, click);
		}
		map.put(id, node);
		return node;
	}
	// Helper function to construct hierarchy
	private void addHierarchy(int child, int parent) {
		Set<Integer> parentSet = hier.get(child);
		if (parentSet == null) {
			parentSet = new TreeSet<Integer>();
		}
		parentSet.add(parent);
		hier.put(child, parentSet);
	}
	// Helper function to build column indices
	private static int buildIndex(Map<String, Integer> map, int offset) {
		for (Entry<String, Integer> e : map.entrySet()) {
			e.setValue(offset++);
		}
		return offset;
	}
}
