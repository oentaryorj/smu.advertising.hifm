package larc.recommender.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import larc.recommender.hierarchy.DirectedGraph;
import larc.recommender.hierarchy.Edge;
import larc.recommender.hierarchy.Node;

public class GraphParser {
	private DirectedGraph<Node, Edge> graph = new DirectedGraph<Node, Edge>();
	private Map<String, Node> nodes = new HashMap<String, Node>();
	
	public GraphParser() {
	}
	public DirectedGraph<Node, Edge> getGraph() {
		return graph;
	}
	public void clear() {
		graph.clear();
		nodes.clear();
	}
	public DirectedGraph<Node, Edge> node(String file, int lineSkips) throws IOException {
		long time = System.currentTimeMillis();
		BufferedReader in = Files.newBufferedReader(Paths.get(file), Charset.forName("UTF-8"));
		for (int row = 0; row < lineSkips; row++) {		// skip comment lines
			in.readLine();
		}
		String line;
		graph.clear();		// reset
		nodes.clear();
		while ((line = in.readLine()) != null) {		// parse file
			String[] str = line.split("[,\t ]");
			if (str.length < 1) {
				throw new IOException("A node should have at least 1 attribute");
			}
			Node node = nodes.get(str[0]);
			if (node == null) {
				node = new Node(str[0]);
				if (str.length >= 2) node.expose = Integer.valueOf(str[1]);
				if (str.length >= 3) node.click = Integer.valueOf(str[2]);
			}
			graph.addVertex(node);
			nodes.put(str[0], node);
		}
		in.close();
		System.out.println("Reading from " + file + " " + graph.getVertexCount() + " nodes takes " + (System.currentTimeMillis() - time) / 1000 + " secs");
		return graph;
	}
	public DirectedGraph<Node, Edge> edge(String file, int lineSkips) throws IOException {
		long time = System.currentTimeMillis();
		BufferedReader in = Files.newBufferedReader(Paths.get(file), Charset.forName("UTF-8"));
		for (int row = 0; row < lineSkips; row++) {		// skip comment lines
			in.readLine();
		}
		String line;
		while ((line = in.readLine()) != null) {		// parse file
			String[] str = line.split("[,\t ]");
			if (str.length < 2) {
				throw new IOException("An edge should have at least 2 attributes");
			}
			Node src = nodes.get(str[0]);
			Node tar = nodes.get(str[1]);
			if (src != null && tar != null) {
				//double weight = (str.length >= 3)? Double.valueOf(str[2]) : 0.0;
				//String type = (str.length >= 4)? str[3] : "";
				if (graph.addEdge(new Edge(), src, tar) == false) {
					throw new IOException("Fail to add edge. Why???");
				}
			}
			else {
				throw new IOException("Nodes do not exist!");
			}
		}
		in.close();
		System.out.println("Reading from " + file + " " + graph.getEdgeCount() + " edges takes " + (System.currentTimeMillis() - time) / 1000 + " secs");
		return graph;
	}
}
