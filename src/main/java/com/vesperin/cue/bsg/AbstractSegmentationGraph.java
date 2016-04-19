package com.vesperin.cue.bsg;

import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.vesperin.cue.spi.DirectedAcyclicGraph;
import com.vesperin.cue.spi.DirectedGraph;
import com.vesperin.cue.spi.Edge;
import com.vesperin.cue.spi.Vertex;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public abstract class AbstractSegmentationGraph implements SegmentationGraph {
  private static final Ordering<Segment> BY_DEPTH = new Ordering<Segment>() {
    public int compare(Segment left, Segment right) {
      return Ints.compare(left.getDepth(), right.getDepth());
    }
  };

  private final DirectedGraph<ASTNode> directedGraph;

  /**
   * Construct a new AbstractSegmentationGraph object using a
   * directed acyclic graph as default implementation.
   */
  protected AbstractSegmentationGraph(){
    this(new DirectedAcyclicGraph<>());
  }

  /**
   * Construct a new AbstractSegmentationGraph object.
   *
   * @param directedGraph a directed graph backing up this BSG.
   */
  protected AbstractSegmentationGraph(DirectedGraph<ASTNode> directedGraph){
    this.directedGraph = directedGraph;
  }

  @Override public boolean addVertex(Vertex<ASTNode> v) {
    return this.directedGraph.addVertex(v);
  }

  @Override public void addRootVertex(Vertex<ASTNode> root) {
    this.directedGraph.addRootVertex(root);
  }

  @Override public boolean addEdge(Vertex<ASTNode> from, Vertex<ASTNode> to) throws IllegalArgumentException {
    return this.directedGraph.addEdge(from, to);
  }

  @Override public boolean addEdge(Vertex<ASTNode> from, Vertex<ASTNode> to, int cost) throws IllegalArgumentException {
    return this.directedGraph.addEdge(from, to, cost);
  }

  @Override public boolean containsEdge(Vertex<ASTNode> from, Vertex<ASTNode> to) {
    return this.directedGraph.containsEdge(from, to);
  }

  @Override public boolean containsVertex(Vertex<ASTNode> vertex) {
    return this.directedGraph.containsVertex(vertex);
  }

  @Override public Vertex<ASTNode> getRootVertex() {
    return this.directedGraph.getRootVertex();
  }

  @Override public Vertex<ASTNode> getVertex(int idx) {
    return this.directedGraph.getVertex(idx);
  }

  @Override public Vertex<ASTNode> getVertex(String label) {
    return this.directedGraph.getVertex(label);
  }

  @Override public List<Vertex<ASTNode>> getVertices() {
    return this.directedGraph.getVertices();
  }

  @Override public List<Edge<ASTNode>> getEdges() {
    return this.directedGraph.getEdges();
  }

  @Override public boolean isRootVertex(Vertex<ASTNode> vertex) {
    return this.directedGraph.isRootVertex(vertex);
  }

  @Override public boolean isEmpty() {
    return this.directedGraph.isEmpty();
  }

  @Override public boolean removeVertex(Vertex<ASTNode> v) {
    return this.directedGraph.removeVertex(v);
  }

  @Override public boolean removeEdge(Vertex<ASTNode> from, Vertex<ASTNode> to) {
    return this.directedGraph.removeEdge(from, to);
  }

  @Override public int size() {
    return this.directedGraph.size();
  }

  @Override public String toString() {
    StringBuilder tmp = new StringBuilder("BSG[");

    List<Segment> segments = getVertices().stream().map(v -> (Segment) v).collect(Collectors.toList());
    segments = BY_DEPTH.sortedCopy(segments);

    for (Segment v : segments) {
      tmp.append(v).append(", ");
    }

    tmp.append(']');
    return tmp.toString();
  }
}
