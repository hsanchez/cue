package com.vesperin.cue.graph;

import com.google.common.collect.Sets;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
interface DirectedGraph <V, E extends Edge <V>> {
  /**
   * Insert a directed edge into the graph.
   *
   * @param from the starting vertex
   * @param to   the ending vertex
   * @return true if the edge was added, false otherwise.
   * @throws IllegalArgumentException if from/to are not vertices in the graph
   * @throws CycleEdgesException if found a cycle in the graph.
   */
  default boolean addEdge(V from, V to){
    return addEdge(from, to, 0.0);
  }

  /**
   * Insert a directed edge into the graph.
   *
   * @param from the starting vertex
   * @param to   the ending vertex
   * @param weight the edge weight/cost
   * @return true if the edge was added, false otherwise.
   * @throws IllegalArgumentException if from/to are not vertices in the graph
   */
  default boolean addEdge(V from, V to, double weight){
    if (!containsVertex(from))
      throw new IllegalArgumentException("from is not in graph");
    if (!containsVertex(to))
      throw new IllegalArgumentException("to is not in graph");

    final E e = edgeFactory().make(from, to, weight);
    if (containsEdge(from, to)) { return false; } else {
      edgeSet().add(e);
      return true;
    }
  }

  /**
   * Adds the specified vertex to this graph if not already present. If this graph
   * already contains such vertex, the call leaves this graph unchanged and returns
   * <tt>false</tt>.
   *
   * @param v vertex to be added to this graph.
   *
   * @return true if this graph did not already contain the specified
   * vertex; false otherwise.
   *
   * @throws NullPointerException if the specified vertex is null.
   */
  default boolean addVertex(V v){
    boolean added = false;
    if (!containsVertex(v)) {
      added = vertexSet().add(v);
    }

    return added;
  }

  /**
   * Set a root vertex. If root does no exist in the graph it is added.
   *
   * @param v vertex to set as the root and optionally
   * add if it does not exist in the graph.
   *
   * @return true if this graph did not contain the specified vertex;
   * false otherwise.
   *
   * @throws NullPointerException if the specified vertex is null.
   */
  boolean addRootVertex(V v);

  /**
   * Returns <tt>true</tt> if and only if this graph contains an edge going
   * from the source vertex to the target vertex. In undirected graphs the
   * same result is obtained when source and target are inverted. If any of
   * the specified vertices does not exist in the graph, or if is <code>
   * null</code>, returns <code>false</code>.
   *
   * @param sourceVertex source vertex of the edge.
   * @param targetVertex target vertex of the edge.
   *
   * @return <tt>true</tt> if this graph contains the specified edge.
   */
  default boolean containsEdge(V sourceVertex, V targetVertex){
    final Set<E> out = outgoingEdgesOf(sourceVertex);
    final Set<E> in  = incomingEdgesOf(targetVertex);

    return Sets.intersection(out, in).size() == 1;
  }

  /**
   * Returns <tt>true</tt> if this graph contains the specified edge. More
   * formally, returns <tt>true</tt> if and only if this graph contains an
   * edge <code>e2</code> such that <code>e.equals(e2)</code>. If the
   * specified edge is <code>null</code> returns <code>false</code>.
   *
   * @param e edge whose presence in this graph is to be tested.
   *
   * @return <tt>true</tt> if this graph contains the specified edge.
   */
  default boolean containsEdge(E e){
    return edgeSet().contains(e);
  }

  /**
   * Returns <tt>true</tt> if this graph contains the specified vertex. More
   * formally, returns <tt>true</tt> if and only if this graph contains a
   * vertex <code>u</code> such that <code>u.equals(v)</code>. If the
   * specified vertex is <code>null</code> returns <code>false</code>.
   *
   * @param v vertex whose presence in this graph is to be tested.
   *
   * @return <tt>true</tt> if this graph contains the specified vertex.
   */
  default boolean containsVertex(V v){
    return vertexSet().contains(v);
  }

  /**
   * Gets the factory object responsible for making edges.
   *
   * @return the current {@link EdgeFactory} object.
   */
  EdgeFactory<V, E> edgeFactory();

  /**
   * Returns a set of the edges contained in this graph. The set is backed by
   * the graph, so changes to the graph are reflected in the set. If the graph
   * is modified while an iteration over the set is in progress, the results
   * of the iteration are undefined.
   *
   * <p>The graph implementation may maintain a particular set ordering (e.g.
   * via {@link java.util.LinkedHashSet}) for deterministic iteration, but
   * this is not required. It is the responsibility of callers who rely on
   * this behavior to only use graph implementations which support it.</p>
   *
   * @return a set of the edges contained in this graph.
   */
  Set<E> edgeSet();

  /**
   * Returns a set of all edges touching the specified vertex. If no edges are
   * touching the specified vertex returns an empty set.
   *
   * @param vertex the vertex for which a set of touching edges is to be
   * returned.
   *
   * @return a set of all edges touching the specified vertex.
   *
   * @throws IllegalArgumentException if vertex is not found in the graph.
   * @throws NullPointerException if vertex is <code>null</code>.
   */
  default Set<E> edgesOf(V vertex) {
    final Set<E> out = outgoingEdgesOf(vertex);
    final Set<E> in  = incomingEdgesOf(vertex);

    return Sets.union(out, in).immutableCopy();
  }

  /**
   * Search the outgoing edges looking for an edge whose edge.to == dest.
   *
   * @param src the source
   * @param dest the destination
   * @return the outgoing edge going to dest if one exists, null otherwise.
   */
  default E findEdge(V src, V dest) {
    for (E e : edgesOf(src)) {
      if (e.to().equals(dest))
        return e;
    }

    return null;
  }

  /**
   * Returns the weight assigned to a given edge. Unweighted graphs return 0.0.
   *
   * @param src the source
   * @param dest the destination
   *
   * @return edge weight
   */
  default double getEdgeWeight(V src, V dest) {
    if (dest == src)
      return 0.0;

    E e = findEdge(src, dest);
    return getEdgeWeight(e);
  }


  /**
   * Returns the weight assigned to a given edge. Unweighted graphs return 0.0.
   *
   * @param e edge of interest
   *
   * @return edge weight
   */
  default double getEdgeWeight(E e) {
    double cost = Integer.MAX_VALUE;
    if (e != null)
      cost = e.weight();
    return cost;
  }

  /**
   * Returns the root vertex of the graph.
   *
   * @return the current root of the graph; or null if none.
   */
  V getRootVertex();


  /**
   * Returns the "in degree" of the specified vertex. An in degree of a vertex
   * in a directed graph is the number of inward directed edges from that
   * vertex. See <a href="http://mathworld.wolfram.com/Indegree.html">
   * http://mathworld.wolfram.com/Indegree.html</a>.
   *
   * @param vertex vertex whose degree is to be calculated.
   *
   * @return the degree of the specified vertex.
   */
  default int inDegreeOf(V vertex){
    return incomingEdgesOf(vertex).size();
  }


  /**
   * Returns whether this vertex is a root vertex.
   *
   * @param vertex the vertex to be inspected.
   * @return true if this is a root, false otherwise.
   */
  boolean isRootVertex(V vertex);

  /**
   * Returns a set of all edges incoming into the specified vertex.
   *
   * @param vertex the vertex for which the list of incoming edges to be
   * returned.
   *
   * @return a set of all edges incoming into the specified vertex.
   */
  default Set<E> incomingEdgesOf(V vertex){
    return edgeSet().stream()
      .filter(e -> e.to().equals(vertex))
      .collect(Collectors.toSet());
  }

  /**
   * Returns the "out degree" of the specified vertex. An out degree of a
   * vertex in a directed graph is the number of outward directed edges from
   * that vertex. See <a href="http://mathworld.wolfram.com/Outdegree.html">
   * http://mathworld.wolfram.com/Outdegree.html</a>.
   *
   * @param vertex vertex whose degree is to be calculated.
   *
   * @return the degree of the specified vertex.
   */
  default int outDegreeOf(V vertex){
    return outgoingEdgesOf(vertex).size();
  }

  /**
   * Returns a set of all edges outgoing from the specified vertex.
   *
   * @param vertex the vertex for which the list of outgoing edges to be
   * returned.
   *
   * @return a set of all edges outgoing from the specified vertex.
   */
  default Set<E> outgoingEdgesOf(V vertex) {
    return edgeSet().stream()
      .filter(e -> e.from().equals(vertex))
      .collect(Collectors.toSet());
  }

  /**
   * Removes an edge going from source vertex to target vertex, if such
   * vertices and such edge exist in this graph. Returns the edge if removed
   * or <code>null</code> otherwise.
   *
   * @param sourceVertex source vertex of the edge.
   * @param targetVertex target vertex of the edge.
   *
   * @return true if the edge was removed; false otherwise.
   */
  default boolean removeEdge(V sourceVertex, V targetVertex){
    E e = findEdge(sourceVertex, targetVertex);
    if (e == null) { return false; } else {
      edgeSet().remove(e);
      return true;
    }
  }

  /**
   * Removes the specified edge from the graph. Removes the specified edge
   * from this graph if it is present. More formally, removes an edge <code>
   * e2</code> such that <code>e2.equals(e)</code>, if the graph contains such
   * edge. Returns <tt>true</tt> if the graph contained the specified edge.
   * (The graph will not contain the specified edge once the call returns).
   *
   * <p>If the specified edge is <code>null</code> returns <code>
   * false</code>.</p>
   *
   * @param e edge to be removed from this graph, if present.
   *
   * @return <code>true</code> if and only if the graph contained the
   * specified edge.
   */
  default boolean removeEdge(E e) {
    return containsEdge(e) && edgeSet().remove(e);
  }

  /**
   * Removes the specified vertex from this graph including all its touching
   * edges if present. More formally, if the graph contains a vertex <code>
   * u</code> such that <code>u.equals(v)</code>, the call removes all edges
   * that touch <code>u</code> and then removes <code>u</code> itself. If no
   * such <code>u</code> is found, the call leaves the graph unchanged.
   * Returns <tt>true</tt> if the graph contained the specified vertex. (The
   * graph will not contain the specified vertex once the call returns).
   *
   * <p>If the specified vertex is <code>null</code> returns <code>
   * false</code>.</p>
   *
   * @param v vertex to be removed from this graph, if present.
   *
   * @return <code>true</code> if the graph contained the specified vertex;
   * <code>false</code> otherwise.
   */
  boolean removeVertex(V v);

  /**
   * The size of the graph is |E|; i.e., the number of edges in the
   * graph.
   *
   * @return the number of edges this graph has.
   */
  default int size(){
    return edgeSet().size();
  }

  /**
   * Returns a set of the vertices contained in this graph. The set is backed
   * by the graph, so changes to the graph are reflected in the set. If the
   * graph is modified while an iteration over the set is in progress, the
   * results of the iteration are undefined.
   *
   * <p>The graph implementation may maintain a particular set ordering (e.g.
   * via {@link java.util.LinkedHashSet}) for deterministic iteration, but
   * this is not required. It is the responsibility of callers who rely on
   * this behavior to only use graph implementations which support it.</p>
   *
   * @return a set view of the vertices contained in this graph.
   */
  Set<V> vertexSet();

  /**
   * Edge generator type.
   *
   * @param <V> vertex type
   * @param <E> edge type
   */
  interface EdgeFactory <V, E extends Edge<V>> {
    /**
     * Makes a new edge.
     *
     * @param from the source vertex
     * @param to the destination vertex
     * @param weight the vertex's weight/cost
     * @return a new edge object.
     */
    E make(V from, V to, double weight);
  }
}
