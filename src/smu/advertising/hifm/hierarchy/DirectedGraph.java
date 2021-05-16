package larc.recommender.hierarchy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import larc.recommender.base.Pair;

public class DirectedGraph<V,E> extends AbstractGraph<V,E> {
	private static final long serialVersionUID = -4921124953541994437L;
	protected final Map<V, Pair<Map<V,E>>> vertices;  // Map of vertices to Pair of adjacency maps {incoming, outgoing} of neighboring vertices to incident edges
    protected final Map<E, Pair<V>> edges;            // Map of edges to incident vertex pairs
    protected final Set<E> emptyEdge = new HashSet<E>();
    protected final Set<V> emptyVertex = new HashSet<V>();
    
    public DirectedGraph() {
        this.vertices = new HashMap<V, Pair<Map<V,E>>>();
        this.edges = new HashMap<E, Pair<V>>();
    }
	@Override
    public void clear() {
		for (Entry<V, Pair<Map<V,E>>> e : this.vertices.entrySet()) {
			Pair<Map<V,E>> pair = e.getValue();
			pair.getFirst().clear();
			pair.getSecond().clear();
		}
    	this.vertices.clear();
    	this.edges.clear();
    }
    @Override
    public boolean addEdge(E edge, Pair<? extends V> endpoints) {
    	Pair<V> new_endpoints = getValidatedEndpoints(edge, endpoints);
        if (new_endpoints == null) {
            return false;
        }
        V source = new_endpoints.getFirst();
        V dest = new_endpoints.getSecond();
        if (findEdge(source, dest) != null) {
            return false;
        }
        edges.put(edge, new_endpoints);
        if (!vertices.containsKey(source)) {
            this.addVertex(source);
        }
        if (!vertices.containsKey(dest)) {
            this.addVertex(dest);
        }
        // map source of this edge to <dest, edge> and vice versa
        vertices.get(source).getSecond().put(dest, edge);
        vertices.get(dest).getFirst().put(source, edge);
        return true;
    }
    @Override
    public E findEdge(V v1, V v2) {
        return (containsVertex(v1) && containsVertex(v2))? vertices.get(v1).getSecond().get(v2) : null;
    }
    @Override
    public Collection<E> findEdgeSet(V v1, V v2) {
        if (!containsVertex(v1) || !containsVertex(v2)) {
            return null;
        }
        ArrayList<E> edge_collection = new ArrayList<E>(1);
        E e = findEdge(v1, v2);
        if (e == null) {
            return edge_collection;
        }
        edge_collection.add(e);
        return edge_collection;
    }
    protected Collection<E> getIncoming_internal(V vertex) {
        return vertices.get(vertex).getFirst().values();
    }
    protected Collection<E> getOutgoing_internal(V vertex) {
        return vertices.get(vertex).getSecond().values();
    }
    protected Collection<V> getPreds_internal(V vertex) {
        return vertices.get(vertex).getFirst().keySet();
    }
    protected Collection<V> getSuccs_internal(V vertex) {
        return vertices.get(vertex).getSecond().keySet();
    }
    @Override
    public Collection<E> getInEdges(V vertex) {
        //return containsVertex(vertex)? Collections.unmodifiableCollection(getIncoming_internal(vertex)) : emptyEdge;
    	return containsVertex(vertex)? getIncoming_internal(vertex) : emptyEdge;
    }
    @Override
    public Collection<E> getOutEdges(V vertex) {
        //return containsVertex(vertex)? Collections.unmodifiableCollection(getOutgoing_internal(vertex)) : emptyEdge;
    	return containsVertex(vertex)? getOutgoing_internal(vertex) : emptyEdge;
    }
    @Override
    public Collection<V> getPredecessors(V vertex) {
        //return containsVertex(vertex)? Collections.unmodifiableCollection(getPreds_internal(vertex)) : emptyVertex;
    	return containsVertex(vertex)? getPreds_internal(vertex) : emptyVertex;
    }
    @Override
    public Collection<V> getSuccessors(V vertex) {
        //return containsVertex(vertex)? Collections.unmodifiableCollection(getSuccs_internal(vertex)) : emptyVertex;
    	return containsVertex(vertex)? getSuccs_internal(vertex) : emptyVertex;
    }
    @Override
    public Pair<V> getEndpoints(E edge) {
        return containsEdge(edge)? edges.get(edge) : null;
    }
    @Override
    public V getSource(E directed_edge) {
        return containsEdge(directed_edge)? edges.get(directed_edge).getFirst() : null;
    }
    @Override
    public V getDest(E directed_edge) {
        return containsEdge(directed_edge)? edges.get(directed_edge).getSecond() : null;
    }
    @Override
    public boolean isSource(V vertex, E edge) {
        return (containsEdge(edge) && containsVertex(vertex))? vertex.equals(this.getEndpoints(edge).getFirst()) : false;
    }
    @Override
    public boolean isDest(V vertex, E edge) {
        return (containsEdge(edge) && containsVertex(vertex))? vertex.equals(this.getEndpoints(edge).getSecond()) : false;
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
        if (!containsVertex(vertex)) {
            return null;   
        }
        Collection<V> neighbors = new HashSet<V>();
        neighbors.addAll(getPreds_internal(vertex));
        neighbors.addAll(getSuccs_internal(vertex));
        return Collections.unmodifiableCollection(neighbors);
    }
    @Override
    public Collection<E> getIncidentEdges(V vertex) {
        if (!containsVertex(vertex)) {
            return null;
        }
        Collection<E> incident_edges = new HashSet<E>();
        incident_edges.addAll(getIncoming_internal(vertex));
        incident_edges.addAll(getOutgoing_internal(vertex));
        return Collections.unmodifiableCollection(incident_edges);
    }
    @Override
    public boolean addVertex(V vertex) {
        if (vertex == null) {
            throw new IllegalArgumentException("vertex may not be null");
        }
        if (!containsVertex(vertex)) {
            vertices.put(vertex, new Pair<Map<V,E>>(new HashMap<V,E>(), new HashMap<V,E>()));
            return true;
        } 
        return false;
    }
    @Override
    public boolean removeVertex(V vertex) {
        if (!containsVertex(vertex)) {
            return false;  
        }
        // copy to avoid concurrent modification in removeEdge
        ArrayList<E> incident = new ArrayList<E>(getIncoming_internal(vertex));
        incident.addAll(getOutgoing_internal(vertex));
        for (E edge : incident) {
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
        Pair<V> endpoints = this.getEndpoints(edge);
        V source = endpoints.getFirst();
        V dest = endpoints.getSecond();
        // remove vertices from each others' adjacency maps
        vertices.get(source).getSecond().remove(dest);
        vertices.get(dest).getFirst().remove(source);
        edges.remove(edge);
        return true;
    }
}