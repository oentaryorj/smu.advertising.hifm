package larc.recommender.hierarchy;

import java.util.*;

import larc.recommender.base.Pair;

public class UndirectedGraph<V, E> extends AbstractGraph<V, E> {
	private static final long serialVersionUID = -5390813832363040089L;
	
	protected Map<V, Map<V,E>> vertices; // Map of vertices to adjacency maps of vertices to incident edges
	protected Map<E, Pair<V>> edges;    // Map of edges to incident vertex sets
	
	public UndirectedGraph() {
		this.vertices = new HashMap<V, Map<V,E>>();
		this.edges = new HashMap<E, Pair<V>>();
	}
	@Override
    public void clear() {
		this.vertices.clear();
		this.edges.clear();
    }
	@Override
	public boolean addEdge(E edge, Pair<? extends V> endpoints) {
		Pair<V> new_endpoints = getValidatedEndpoints(edge, endpoints);
	    if (new_endpoints == null) {
	        return false;
	    }
	    V v1 = new_endpoints.getFirst();
	    V v2 = new_endpoints.getSecond();
	    if (findEdge(v1, v2) != null) {
	        return false;
	    }
	    edges.put(edge, new_endpoints);
	    if (!vertices.containsKey(v1)) {
	        this.addVertex(v1);
	    }
	    if (!vertices.containsKey(v2)) {
	        this.addVertex(v2);
	    }
	    // map v1 to <v2, edge> and vice versa
	    vertices.get(v1).put(v2, edge);
	    vertices.get(v2).put(v1, edge);
	    return true;
	}
	public Collection<E> getInEdges(V vertex) {
	    return this.getIncidentEdges(vertex);
	}
	public Collection<E> getOutEdges(V vertex) {
	    return this.getIncidentEdges(vertex);
	}
	public Collection<V> getPredecessors(V vertex) {
	    return this.getNeighbors(vertex);
	}
	public Collection<V> getSuccessors(V vertex) {
	    return this.getNeighbors(vertex);
	}
	@Override
	public E findEdge(V v1, V v2) {
	    return (containsVertex(v1) && containsVertex(v2))? vertices.get(v1).get(v2) : null;
	}
	@Override
	public Collection<E> findEdgeSet(V v1, V v2) {
	    if (!containsVertex(v1) || !containsVertex(v2)) {
	        return null;
	    }
	    ArrayList<E> edge_collection = new ArrayList<E>(1);
	//    if (!containsVertex(v1) || !containsVertex(v2))
	//        return edge_collection;
	    E e = findEdge(v1, v2);
	    if (e == null) {
	        return edge_collection;
	    }
	    edge_collection.add(e);
	    return edge_collection;
	}
	@Override
	public Pair<V> getEndpoints(E edge) {
	    return edges.get(edge);
	}
	@Override
	public V getSource(E directed_edge) {
	    return null;
	}
	@Override
	public V getDest(E directed_edge) {
	    return null;
	}
	@Override
	public boolean isSource(V vertex, E edge) {
	    return false;
	}
	@Override
	public boolean isDest(V vertex, E edge) {
	    return false;
	}
	@Override
	public Collection<E> getEdges() {
	    return Collections.unmodifiableCollection(edges.keySet());
	}
	@Override
	public Collection<V> getVertices() {
	    return Collections.unmodifiableCollection(vertices.keySet());
	}
	@Override
	public boolean containsVertex(V vertex) {
	    return vertices.containsKey(vertex);
	}
	@Override
	public boolean containsEdge(E edge) {
	    return edges.containsKey(edge);
	}
	@Override
	public int getEdgeCount() {
	    return edges.size();
	}
	@Override
	public int getVertexCount() {
	    return vertices.size();
	}
	@Override
	public Collection<V> getNeighbors(V vertex) {
	    return containsVertex(vertex)? Collections.unmodifiableCollection(vertices.get(vertex).keySet()) : null;
	}
	@Override
	public Collection<E> getIncidentEdges(V vertex) {
	    return containsVertex(vertex)? Collections.unmodifiableCollection(vertices.get(vertex).values()) : null;
	}
	@Override
	public boolean addVertex(V vertex) {
	    if (vertex == null) {
	        throw new IllegalArgumentException("vertex may not be null");
	    }
	    if (!containsVertex(vertex)) {
	        vertices.put(vertex, new HashMap<V,E>());
	        return true;
	    } else {
	        return false;
	    }
	}
	@Override
	public boolean removeVertex(V vertex) {
	    if (!containsVertex(vertex)) {
	        return false;
	    }
	    // iterate over copy of incident edge collection
	    for (E edge : new ArrayList<E>(vertices.get(vertex).values())) {
	        removeEdge(edge);
	    }
	    vertices.remove(vertex);
	    return true;
	}
	@Override
	public boolean removeEdge(E edge) {
	    if (!containsEdge(edge)) {
	        return false;
	    }
	    Pair<V> endpoints = getEndpoints(edge);
	    V v1 = endpoints.getFirst();
	    V v2 = endpoints.getSecond();
	    // remove incident vertices from each others' adjacency maps
	    vertices.get(v1).remove(v2);
	    vertices.get(v2).remove(v1);
	    edges.remove(edge);
	    return true;
	}
}