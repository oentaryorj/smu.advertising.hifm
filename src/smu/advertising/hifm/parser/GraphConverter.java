package larc.recommender.parser;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import larc.recommender.base.Pair;
import larc.recommender.data.Data;
import larc.recommender.data.Feature;
import larc.recommender.data.Instance;
import larc.recommender.hierarchy.DirectedGraph;
import larc.recommender.hierarchy.Edge;
import larc.recommender.hierarchy.Hierarchy;
import larc.recommender.hierarchy.Node;

public class GraphConverter {
	private final DirectedGraph<Node, Edge> graph;
	private final List<Node> leaves = new LinkedList<Node>();
	private final List<Node> nonLeaves = new LinkedList<Node>();
	private final Map<String, Integer> users = new TreeMap<String, Integer>();
	private final Map<String, Integer> items = new TreeMap<String, Integer>();
	private final Map<String, Integer> times = new TreeMap<String, Integer>();
	private final Map<String, Integer> userParents = new TreeMap<String, Integer>();
	private final Map<String, Integer> itemParents = new TreeMap<String, Integer>();
	private final String delim;
	private final String ignore;
	
	public GraphConverter(DirectedGraph<Node, Edge> graph, String delim, String ignore) throws Exception {
		this.graph = graph;
		this.delim = delim;
		this.ignore = ignore;
		for (Node node : graph.getVertices()) {
			String[] ids = node.id.split(delim);
			if (ids.length != 3) {
				throw new Exception("Node ids should be of length 3");
			}
			if (graph.outDegree(node) == 0) {		// leaf nodes
				users.put(ids[0], 0);
				items.put(ids[1], 0);
				times.put(ids[2], 0);
				leaves.add(node);
			} else {										// non-leaf nodes
				if (!ids[0].equals(ignore))
					userParents.put(ids[0], 0);
				if (!ids[1].equals(ignore)) 
					itemParents.put(ids[1], 0);
				//if (!ids[2].equals(ignore)) 
				//	timeParents.put(ids[2], 0);
				nonLeaves.add(node);
			}
		}
		System.out.println("Users: " + users.size() + " items: " + items.size() + " times: " + times.size() + " user parents: " + userParents.size() + " item parents: " + itemParents.size());
		int colOffset = 0;
		colOffset = buildIndex(users, colOffset);
		colOffset = buildIndex(items, colOffset);
		colOffset = buildIndex(times, colOffset);
		colOffset = buildIndex(userParents, colOffset);
		colOffset = buildIndex(itemParents, colOffset);
		//colOffset = buildIndex(timeParents, colOffset);
	}
	public void clear() {
		graph.clear();
		leaves.clear();
		nonLeaves.clear();
		users.clear();
		items.clear();
		times.clear();
		userParents.clear();
		itemParents.clear();
	}
	public Data toData(String startDay, String endDay) throws Exception {
		if (startDay.compareTo(endDay) > 0) {
			throw new Exception("Start day is later than end day!");
		}
		long time = System.currentTimeMillis();
		List<Node> chosenLeaves = new LinkedList<Node>();
		for (Node leaf : leaves) {
			String[] ids = leaf.id.split(delim);
			if (ids[2].compareTo(startDay) >= 0 && ids[2].compareTo(endDay) <= 0) {
				chosenLeaves.add(leaf);
			}
		}
		int numCols = users.size() + items.size() + times.size() + userParents.size() + itemParents.size(); /*+ timeParents.size());*/
		Data data = new Data(chosenLeaves.size(), numCols); 	// reset
		HashMap<String, Integer> shards = new HashMap<String, Integer>();
		int row = 0;
		//double sumWeight = 0.0;
		for (Node leaf : chosenLeaves) {
			String[] ids = leaf.id.split(delim);
			data.set(row, users.get(ids[0]), 1.0);
			data.set(row, items.get(ids[1]), 1.0);
			data.set(row, times.get(ids[2]), 1.0);
			String shard = ids[0] + "," + ids[2]; 
			if (!shards.containsKey(shard)) {
				shards.put(shard, shards.size());
			}
			Instance x = data.row(row);
			x.shard = shards.get(shard);
			x.target = leaf.value();
			x.id = leaf.id + "," + leaf.expose + "," + leaf.click;
			x.weight = leaf.expose;
			//x.weight = 1;
			//sumWeight += leaf.expose;
			appendParents(leaf, x);		// set the columns for parent nodes
			++row;
		}
		/*double scale = (double) data.getNumRows() / sumWeight; 
		for (row = 0; row < data.getNumRows(); row++) {	// normalize weight
			data.row(row).weight *= scale;
		}*/
		System.out.println("Translating to data with " + data.getNumRows() + " rows and " + data.getNumCols() + " cols takes " + (System.currentTimeMillis() - time) / 1000 + " secs");
		return data;
	}
	// Capture the hierarchical structure
	public Hierarchy toHierarchy() {
		Map<Integer, Set<Integer>> map = new TreeMap<Integer, Set<Integer>>();
		for (Edge edge : graph.getEdges()) {
			Pair<Node> pair = graph.getEndpoints(edge);
			String[] src = pair.getFirst().id.split(delim);
			String[] dest = pair.getSecond().id.split(delim);
			int srcKey = -1, destKey = -1;
			if (!src[0].equals(ignore)) {
				srcKey = userParents.get(src[0]);
				destKey = users.containsKey(dest[0])? users.get(dest[0]) : userParents.get(dest[0]);
			}
			if (!src[1].equals(ignore)) {
				srcKey = itemParents.get(src[1]);
				destKey = items.containsKey(dest[1])? items.get(dest[1]) : itemParents.get(dest[1]);
			}
			//if (!src[2].equals(ignore)) {
			//	srcKey = timeParents.get(src[2]);
			//	destKey = times.containsKey(dest[2])? times.get(dest[2]) : timeParents.get(dest[2]);
			//}
			Set<Integer> set = map.get(destKey);
			if (set == null) {
				set = new TreeSet<Integer>();
			}
			set.add(srcKey);
			map.put(destKey, set);
		}
		Hierarchy hier = new Hierarchy();
		for (Entry<Integer, Set<Integer>> e : map.entrySet()) {
			List<Integer> list = hier.get(e.getKey());
			if (list == null) {
				list = new LinkedList<Integer>();
				list.addAll(e.getValue());
			}
			hier.put(e.getKey(), list);
		}
		return hier;
	}
	// Agglomerate the exposes and clicks in the edges
	public GraphConverter agglomerate(String startDay, String endDay) {
		for (Node nonLeaf : nonLeaves) {
			nonLeaf.click = nonLeaf.expose = 0;		// reset
		}
		for (Node leaf : leaves) {
			String[] ids = leaf.id.split(delim);
			if (ids[2].compareTo(startDay) >= 0 && ids[2].compareTo(endDay) <= 0) {
				agglomerateStub(leaf);
			}
		}
		return this;
	}
	private void agglomerateStub(Node node) {
		Collection<Node> parents = graph.getPredecessors(node);
		if (parents == null || parents.size() == 0) {
			return;
		}
		for (Node parent : parents) {
			parent.expose += node.expose;
			parent.click += node.click;
			agglomerateStub(parent);			// depth-first search
		}
	}
	private void appendParents(Node node, Instance x) {
		Collection<Edge> inEdges = graph.getInEdges(node);
		if (inEdges == null || inEdges.size() == 0) {
			return;
		}
		for (Edge edge : inEdges) {
			Node parent = graph.getOpposite(node, edge);
			String[] ids = parent.id.split(delim);
			if (!ids[0].equals(ignore)) {
				x.add(new Feature(userParents.get(ids[0]), parent.value()));
			} 
			if (!ids[1].equals(ignore)) {
				x.add(new Feature(itemParents.get(ids[1]), parent.value()));
			}
			appendParents(parent, x);
		}
	}
	private static int buildIndex(Map<String, Integer> set, int offset) {
		for (Entry<String, Integer> e : set.entrySet()) {
			e.setValue(offset++);
		}
		return offset;
	}
}
