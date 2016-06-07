package com.vesperin.cue.segment;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.cue.spi.AbstractDirectedAcyclicGraph;
import com.vesperin.cue.spi.DirectedAcyclicGraph;
import com.vesperin.cue.spi.Edge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
class BlockSegmentationGraph extends
  AbstractDirectedAcyclicGraph<Segment, Edge<Segment>> implements SegmentationGraph {

  private static final Ordering<Segment> BY_DEPTH = new Ordering<Segment>() {
    public int compare(Segment left, Segment right) {
      return Ints.compare(left.depth(), right.depth());
    }
  };

  /**
   * Color used to mark unvisited nodes
   */
  private static final int VISIT_COLOR_WHITE = 1;

  /**
   * Color used to mark nodes as they are first visited in DFS order
   */
  private static final int VISIT_COLOR_GREY = 2;

  /**
   * Color used to mark nodes after descendants are completely visited
   */
  private static final int VISIT_COLOR_BLACK = 4;

  /**
   * Constructs an empty block segmentation graph (DAG).
   */
  BlockSegmentationGraph(){
    this(new EdgeFactoryImpl<>());
  }
  /**
   * Constructs an empty block segmentation graph (DAG) with a predefined
   * edge factory object.
   *
   * @param edgeFactory application specific object for creating edges.
   * @throws NullPointerException if providing a null edge factory
   */
  private BlockSegmentationGraph(EdgeFactory<Segment, Edge<Segment>> edgeFactory) {
    super(edgeFactory);
  }

  @Override public boolean hasCycle() {
    return !findCycles(this).isEmpty();
  }

  @Override public Segment segmentBy(String label) {
    return vertexSet().stream()
      .filter(v -> Objects.equals(v.label(), label))
      .findFirst().orElse(null);
  }

  private static List<Edge<Segment>> findCycles(DirectedAcyclicGraph<Segment, Edge<Segment>> graph) {

    final List<Edge<Segment>> cycleEdges = new ArrayList<>();
    for(Segment each : graph.vertexSet()){
      // Mark all vertices as white
      each.setMarkState(VISIT_COLOR_WHITE);
    }

    for(Segment each : graph.vertexSet()){
      visit(each, graph, cycleEdges);
    }

    return cycleEdges;
  }



  private static void visit(Segment v, DirectedAcyclicGraph<Segment, Edge<Segment>> graph,
          List<Edge<Segment>> cycleEdges) {

    v.setMarkState(VISIT_COLOR_GREY);

    for(Edge<Segment> each : graph.outgoingEdgesOf(v)){
      Segment u = each.to();
      if(u.markState() == VISIT_COLOR_GREY) {
        cycleEdges.add(each);
      } else if (u.markState() == VISIT_COLOR_WHITE){
        visit(u, graph, cycleEdges);
      }
    }

    v.setMarkState(VISIT_COLOR_BLACK);
  }

  @Override public Segment segmentAt(int n) {
    return Iterables.get(vertexSet(), n);
  }

  @Override public Set<Location> irrelevantSet(int capacity) {
    if(capacity <= 3) return ImmutableSet.of();

    final List<Segment> blackList = generateBlackList(this, capacity);

    return blackList.stream()
      .map(foldable -> Locations.locate(foldable.data()))
      .collect(toImmutableSet());
  }

  /**
   * Creates a collector that transforms a mutable set into an immutable one.
   *
   * @param <T> the type parameter.
   * @return a new collector object.
   */
  private static <T> Collector<T, ?, Set<T>> toImmutableSet() {
    return Collectors.collectingAndThen(
      Collectors.toSet(),
      Collections::unmodifiableSet
    );
  }

  private static List<Segment> generateBlackList(SegmentationGraph graph, int capacity) {
    final List<Segment> allSegments = Lists.newLinkedList(
      graph.vertexSet().stream()
        .filter(v -> !(Objects.equals(v, graph.getRootVertex())))
        .map(v -> v)
        .collect(Collectors.toList())
    );

    final int N = allSegments.size();

    // if single Block node, then return empty list
    if(N == 1) return Lists.newArrayList();

    @SuppressWarnings("UnnecessaryLocalVariable")
    final int W = capacity;

    double[] profit = new double[N + 1];
    double[] weight = new double[N + 1];

    // add vertices values
    for( int n = 1; n <= N; n++){
      profit[n] = graph.segmentAt(n).benefit();
      weight[n] = graph.segmentAt(n).weight();
    }

    double[][]  opt = new double [N + 1][W + 1];
    boolean[][] sol = new boolean[N + 1][W + 1];

    for(int n = 1; n <= N; n++){
      for(int w = 1; w <= W; w++){
        // don't take item n
        double option1 = opt[n-1][w];

        // take item n
        double option2 = Double.NEGATIVE_INFINITY;
        if (weight[n] <= w) {
          int weightReduction = (int)(w - weight[n]);
          option2 = profit[n] + opt[n-1][weightReduction];
        }

        // select better of two options only if there is a precedence relation
        // between item n and n - 1
        opt[n][w] = Math.max(option1, option2);
        sol[n][w] = (option2 > option1) && isPrecedenceConstraintMaintained(opt, n, w, graph);
      }
    }

    // determine which items to take
    boolean[] take = new boolean[N+1];
    for (int n = N, w = W; n > 0; n--) {
      if (sol[n][w]) { take[n] = true;  w = (int)(w - weight[n]); }
      else           { take[n] = false;                    }
    }

    final Set<Segment> keep = Sets.newLinkedHashSet();
    for (int n = 1; n <= N; n++) {
      if (take[n]) {
        keep.add(graph.segmentAt(n));
      }
    }

    allSegments.removeAll(keep);

    return allSegments;
  }

  private static boolean isPrecedenceConstraintMaintained(
    double[][] opt, int i, int j,
    SegmentationGraph graph) {


    final Segment parent = graph.segmentAt(i - 1);
    final Segment child = (graph.size() == i
      ? null
      : graph.segmentAt(i)
    );

    // a graph made of a single node implies the following:
    // - the single node is the root
    // - no precedence constraints can be enforced since it has not parent and no children
    final boolean singleNode    = graph.size() == 1;
    final boolean pass          = singleNode && graph.isRootVertex(parent) && child == null;

    return  (pass) || (opt[i][j] != opt[i - 1][j] && graph.containsEdge(parent, child));
  }

  private static class EdgeFactoryImpl <V extends Segment, E extends Edge<V>> implements EdgeFactory <V, E> {
    @Override public E make(V from, V to, double weight) {
      //noinspection unchecked
      return (E) new EdgeImpl<>(from, to, weight); // unchecked warning
    }
  }

  private static class EdgeImpl <V extends Segment> implements Edge <V> {
    private final V from;
    private final V to;
    private final double  weight;

    /**
     * Private construction of an edge implementation.
     *
     * @param from the source vertex
     * @param to the destination vertex
     * @param weight edge's weight
     */
    EdgeImpl(V from, V to, double weight){
      this.from   = from;
      this.to     = to;
      this.weight = weight;
    }

    @Override public V from() {
      return from;
    }

    @Override public V to() {
      return to;
    }

    @Override public double weight() {
      return weight;
    }

    @Override public String toString() {
      return "Edge (" + from() + ", " + to() + ")";
    }
  }

  @Override public String toString() {
    StringBuilder tmp = new StringBuilder("BlockSegmentationGraph[");

    List<Segment> segments = vertexSet().stream()
      .map(v -> v)
      .collect(Collectors.toList());

    segments = BY_DEPTH.sortedCopy(segments);

    for (Segment v : segments) {
      tmp.append(v).append(", ");
    }

    tmp.append(']');
    return tmp.toString();
  }
}
