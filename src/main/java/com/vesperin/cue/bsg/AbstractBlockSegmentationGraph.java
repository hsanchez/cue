package com.vesperin.cue.bsg;

import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.vesperin.cue.spi.DirectedGraph;
import com.vesperin.cue.spi.Edge;
import com.vesperin.cue.spi.Vertex;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
abstract class AbstractBlockSegmentationGraph implements BlockSegmentationGraph {
  private static final Ordering<Segment> BY_DEPTH = new Ordering<Segment>() {
    public int compare(Segment left, Segment right) {
      return Ints.compare(left.getDepth(), right.getDepth());
    }
  };

  /**
   * Construct a new AbstractBlockSegmentationGraph object. Implementors of
   * this class should call this constructor.
   */
  AbstractBlockSegmentationGraph(){}


  protected abstract DirectedGraph<ASTNode> getDirectedGraph();


  @Override public boolean addVertex(Vertex<ASTNode> v) {
    return getDirectedGraph().addVertex(v);
  }

  @Override public void addRootVertex(Vertex<ASTNode> root) {
    getDirectedGraph().addRootVertex(root);
  }

  @Override public boolean addEdge(Vertex<ASTNode> from, Vertex<ASTNode> to) throws IllegalArgumentException {
    return getDirectedGraph().addEdge(from, to);
  }

  @Override public boolean addEdge(Vertex<ASTNode> from, Vertex<ASTNode> to, int cost) throws IllegalArgumentException {
    return getDirectedGraph().addEdge(from, to, cost);
  }

  @Override public boolean containsEdge(Vertex<ASTNode> from, Vertex<ASTNode> to) {
    return getDirectedGraph().containsEdge(from, to);
  }

  @Override public boolean containsVertex(Vertex<ASTNode> vertex) {
    return getDirectedGraph().containsVertex(vertex);
  }

  @Override public Vertex<ASTNode> getRootVertex() {
    return getDirectedGraph().getRootVertex();
  }

  @Override public Vertex<ASTNode> getVertex(int idx) {
    return getDirectedGraph().getVertex(idx);
  }

  @Override public Vertex<ASTNode> getVertex(String label) {
    return getDirectedGraph().getVertex(label);
  }

  @Override public List<Vertex<ASTNode>> getVertices() {
    return getDirectedGraph().getVertices();
  }

  @Override public List<Edge<ASTNode>> getEdges() {
    return getDirectedGraph().getEdges();
  }

  @Override public boolean isRootVertex(Vertex<ASTNode> vertex) {
    return getDirectedGraph().isRootVertex(vertex);
  }

  @Override public boolean isEmpty() {
    return getDirectedGraph().isEmpty();
  }

  @Override public boolean removeVertex(Vertex<ASTNode> v) {
    return getDirectedGraph().removeVertex(v);
  }

  @Override public boolean removeEdge(Vertex<ASTNode> from, Vertex<ASTNode> to) {
    return getDirectedGraph().removeEdge(from, to);
  }

  @Override public int size() {
    return getDirectedGraph().size();
  }

  @Override public String toString() {
    StringBuilder tmp = new StringBuilder("BSG[");

    List<Segment> segments = getVertices().stream()
      .map(v -> (Segment) v)
      .collect(Collectors.toList());

    segments = BY_DEPTH.sortedCopy(segments);

    for (Segment v : segments) {
      tmp.append(v).append(", ");
    }

    tmp.append(']');
    return tmp.toString();
  }
}
