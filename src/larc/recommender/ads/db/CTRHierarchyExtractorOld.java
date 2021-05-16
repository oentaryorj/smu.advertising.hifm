package larc.recommender.ads.db;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import larc.recommender.ads.db.SQLConnector;
import larc.recommender.hierarchy.Node;

public class CTRHierarchyExtractorOld {
	private final Map<String, Node> nodes = new TreeMap<String, Node>();
	private final Map<Node, Set<Node>> edges = new TreeMap<Node, Set<Node>>();
	private static final Charset charSet = Charset.forName("UTF-8");
	
	public CTRHierarchyExtractorOld() {
	}
	public void readSQL(String schema, String pageTable, String adTable, String startDay, String endDay, int minExposes) throws Exception {
		long startTime = System.currentTimeMillis();
		Connection conn = SQLConnector.connect(schema);
		ResultSet rs = SQLConnector.query(conn, "SELECT p.day, p.partnerid AS publisher, p.cntr, p.cid, p.sumexpose, p.sumclick, p.channel," +
							" a.partnerid AS advertiser, a.bannertype FROM " + pageTable + " AS p, " + adTable + " AS a" +
							" WHERE a.cid = p.cid AND p.day >= '" + startDay + "' AND p.day <= '" + endDay + "' AND  p.sumexpose >= " + minExposes);
		while (rs.next()) {
			String day = rs.getString("day");
			String publisher = rs.getString("publisher");
			String country = rs.getString("cntr");
			String page = publisher + "-" + country;
			String channel = rs.getString("channel");
			String advertiser = rs.getString("advertiser");
			String ad = rs.getString("cid");
			String banner = rs.getString("bannertype");
			Node leafNode = addNode(page + ":" + ad + ":" + day);	
			leafNode.expose = rs.getInt("sumexpose");							// set leaf node data
			leafNode.click = rs.getInt("sumclick");
			
			/*Node publisherNode = addNode(publisher + ":_:" + day, 1);			// Page hierarchy
			Node countryNode = addNode(country + ":_:" + day, 1);
			Node channelNode = addNode(channel + ":_:" + day, 2);*/
			Node publisherNode = addNode(publisher + ":_:_");					// Page hierarchy
			Node countryNode = addNode(country + ":_:_");
			Node channelNode = addNode(channel + ":_:_");
			addEdge(publisherNode, leafNode);
			addEdge(countryNode, leafNode);
			addEdge(channelNode, publisherNode);
			
			/*Node advertiserNode = addNode("_:" + advertiser + ":" + day, 1);	// Ad hierarchy
			Node bannerNode = addNode("_:" + banner + ":" + day, 1);*/
			Node advertiserNode = addNode("_:" + advertiser + ":_");			// Ad hierarchy
			Node bannerNode = addNode("_:" + banner + ":_");
			addEdge(advertiserNode, leafNode);
			addEdge(bannerNode, leafNode);
			
			Node rootNode = addNode("_:_:_");
			addEdge(rootNode, channelNode);
			addEdge(rootNode, countryNode);
			addEdge(rootNode, advertiserNode);
			addEdge(rootNode, bannerNode);
		}
		rs.close();
		conn.close();
		System.out.println("Extracting hierarchy from '" + schema + "." + pageTable + "' and '" + adTable + 
						   "' takes " + (System.currentTimeMillis() - startTime) / 1000 + " secs.");
	}
	// Clear the data structures
	public void clear() {
		for (Entry<Node, Set<Node>> e : this.edges.entrySet()) {
			e.getValue().clear();
		}
		this.nodes.clear();
		this.edges.clear();
	}
	// Output nodes into comma-separated text file
	public void writeNodes(String file) throws IOException {
		long time = System.currentTimeMillis();
		BufferedWriter out = Files.newBufferedWriter(Paths.get(file), charSet);
		for (Entry<String, Node> e : nodes.entrySet()) {
			out.write(e.getValue() + "\n");
		}
		out.close();
		System.out.println("Saving nodes to '" + file + "' takes " + (System.currentTimeMillis() - time) / 1000 + " secs.");
	}
	// Output edges into comma-separated text file
	public void writeEdges(String file) throws IOException {
		long time = System.currentTimeMillis();
		BufferedWriter out = Files.newBufferedWriter(Paths.get(file), charSet);
		for (Entry<Node, Set<Node>> p : edges.entrySet()) {
			Node parent = p.getKey();
			for (Node child : p.getValue()) {
				out.write(parent.id + "," + child.id + "," + 1.0 + "\n");
			}
		}
		out.close();
		System.out.println("Saving edges to '" + file + "' takes " + (System.currentTimeMillis() - time) / 1000 + " secs.");
	}
	// Helper function to insert node into edges
	private Node addNode(String id) {
		Node node = nodes.get(id);
		if (node == null) {
			node = new Node(id);
		}
		nodes.put(id, node);
		return node;
	}
	// Helper function to insert edge into edges
	private void addEdge(Node source, Node target) {
		Set<Node> edgeList = edges.get(source);
		if (edgeList == null) {
			edgeList = new TreeSet<Node>();
		}
		edgeList.add(target);
		edges.put(source, edgeList);
	}
}
