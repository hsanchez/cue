package com.vesperin.cue.spi;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hsanchez@cs.ucsc.edu (Huascar A. Sanchez)
 */
public class DirectedAcyclicGraph<T> implements DirectedGraph<T> {

  /**
   * Vector<Vertex> of graph vertices
   */
  private final List<Vertex<T>> vertices;

  /**
   * Vector<Edge> of edges in the graph
   */
  private final List<Edge<T>> edges;

  /**
   * The vertex identified as the root of the graph
   */
  private Vertex<T> rootVertex;

  /**
   * Construct a new graph without any vertices or edges
   */
  public DirectedAcyclicGraph() {
    vertices = new ArrayList<>();
    edges    = new ArrayList<>();
  }

  /**
   * Search the vertices for one with name.
   *
   * @param graph the directed graph
   * @param name  the vertex name or label
   * @return the first vertex with a matching name, null if no matches are found
   */
  public static <T> Vertex<T> findVertexByName(DirectedGraph<T> graph, String name) {
    Vertex<T> match = null;
    for (Vertex<T> v : graph.getVertices()) {
      if (name.equals(v.getName())) {
        match = v;
        break;
      }
    }
    return match;
  }

  @Override public boolean addVertex(Vertex<T> v) {
    boolean added = false;
    if (!containsVertex(v)) {
      added = vertices.add(v);
    }

    return added;
  }

  @Override public boolean addEdge(Vertex<T> from, Vertex<T> to) throws
    IllegalArgumentException {
    return addEdge(from, to, 0);
  }

  @Override public boolean addEdge(Vertex<T> from, Vertex<T> to, int cost) throws
    IllegalArgumentException {
    if (!containsVertex(from))
      throw new IllegalArgumentException("from is not in graph");
    if (!containsVertex(to))
      throw new IllegalArgumentException("to is not in graph");

    Edge<T> e = new EdgeImpl<>(from, to, cost);
    if (containsEdge(from, to))
      return false;
    else {
      from.addEdge(e);
      to.addEdge(e);
      edges.add(e);
      return true;
    }
  }

  @Override public void addRootVertex(Vertex<T> root) {
    this.rootVertex = root;
    if (!containsVertex(root)) {
      this.addVertex(root);
    }
  }

  @Override public boolean containsVertex(Vertex<T> vertex) {
    return vertices.contains(vertex);
  }

  @Override public boolean containsEdge(Vertex<T> from, Vertex<T> to) {
    return from.findEdge(to) != null;
  }

  @Override public Vertex<T> getRootVertex() {
    return rootVertex;
  }

  @Override public Vertex<T> getVertex(int n) {
    return vertices.get(n);
  }

  @Override public Vertex<T> getVertex(String label) {
    return DirectedAcyclicGraph.findVertexByName(this, label);
  }

  @Override public List<Vertex<T>> getVertices() {
    return this.vertices;
  }

  @Override public List<Edge<T>> getEdges() {
    return this.edges;
  }

  @Override public boolean isEmpty() {
    return vertices.size() == 0;
  }

  @Override public boolean isRootVertex(Vertex<T> vertex) {
    return getRootVertex().equals(vertex);
  }

  @Override public boolean removeVertex(Vertex<T> v) {
    if (!containsVertex(v))
      return false;

    getVertices().remove(v);
    if (v == getRootVertex())
      rootVertex = null;

    // Remove the edges associated with v
    for (Edge<T> each : v.getOutgoingEdges()) {
      v.remove(each);
      Vertex<T> to = each.getTo();
      to.remove(each);
      getEdges().remove(each);
    }

    for (Edge<T> each : v.getIncomingEdges()) {
      v.remove(each);
      Vertex<T> predecessor = each.getFrom();
      predecessor.remove(each);
    }

    return true;
  }

  @Override public boolean removeEdge(Vertex<T> from, Vertex<T> to) {
    Edge<T> e = from.findEdge(to);
    if (e == null)
      return false;
    else {
      from.remove(e);
      to.remove(e);
      getEdges().remove(e);
      return true;
    }
  }

  @Override public int size() {
    return vertices.size();
  }


  @Override public String toString() {
    StringBuilder tmp = new StringBuilder("Graph[");

    for (Vertex<T> v : vertices) {
      tmp.append(v).append(", ");
    }

    tmp.append(']');
    return tmp.toString();
  }

}
