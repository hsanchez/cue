package com.vesperin.cue.graph;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
abstract class AbstractDirectedGraph<V, E extends Edge<V>> implements DirectedGraph <V, E> {

  private final EdgeFactory<V, E>   edgeFactory;
  private final Set<E>              edges;
  private final Set<V>              vertices;
  private V root;

  /**
   * Constructs an empty DAG.
   */
  AbstractDirectedGraph(EdgeFactory<V, E> edgeFactory){
    this(null, edgeFactory);
  }

  /**
   * Construct a new DAG using a vertex as parameter.
   *
   * @param vertex the first vertex in the graph; which
   *               can be the root of the graph.
   * @param edgeFactory edge factory
   */
  private AbstractDirectedGraph(V vertex, EdgeFactory<V, E> edgeFactory){
    this.edgeFactory  = Objects.requireNonNull(edgeFactory);
    this.edges        = new LinkedHashSet<>();
    this.vertices     = new LinkedHashSet<>();

    if(vertex != null){
      addRootVertex(vertex);
    }
  }

  @Override public boolean addRootVertex(V v) {
    this.root = v;

    if (!containsVertex(root)) {
      this.addVertex(root);
      return true;
    }

    return false;
  }

  @Override public EdgeFactory<V, E> edgeFactory() {
    return edgeFactory;
  }

  @Override public Set<E> edgeSet() {
    return edges;
  }

  @Override public V getRootVertex() {
    return root;
  }


  @Override public boolean isRootVertex(V vertex) {
    return Objects.equals(vertex, getRootVertex());
  }


  @Override public boolean removeVertex(V v) {
    if (!containsVertex(v))
      return false;

    vertexSet().remove(v);
    if (v == getRootVertex())
      root = null;

    // Remove the edges associated with v
    for (E each : outgoingEdgesOf(v)) {
      edgeSet().remove(each);
    }

    for (E each : incomingEdgesOf(v)) {
      edgeSet().remove(each);
    }

    return true;
  }

  @Override public Set<V> vertexSet() {
    return vertices;
  }

  @Override public String toString() {
    StringBuilder tmp = new StringBuilder("DirectedGraph[");

    for (V v : vertexSet()) {
      tmp.append(v).append(", ");
    }

    tmp.append(']');
    return tmp.toString();
  }
}
