package com.vesperin.cue.bsg;

import com.vesperin.base.locations.Location;
import com.vesperin.cue.spi.DirectedGraph;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Block Segmentation graph. A block segmentation graph is a directed acyclic graph
 * G = (V, E), where V is the code blocks nodes (Segments) and E is composition relations
 * (or edges) between those blocks.
 *
 * @author Huascar Sanchez
 */
public interface BlockSegmentationGraph extends DirectedGraph <ASTNode> {

  /**
   * Adds a segment to the graph.
   *
   * @param segment the non-null segment to add
   * @return true if the segment was added; false otherwise.
   */
  default boolean add(Segment segment){
    return addVertex(Objects.requireNonNull(segment));
  }

  /**
   * Adds the root of this graph.
   *
   * @param segment the non-null root segment.
   */
  default void addRoot(Segment segment){
    addRootVertex(
      Objects.requireNonNull(segment)
    );
  }

  /**
   * Establish a composition relation between two segments in the graph.
   *
   * @param from the whole segment
   * @param to the part to be included in the whole segment.
   * @return true if the relation was established; false otherwise.
   * @throws IllegalArgumentException if from/to are not segments in the graph
   */
  default boolean connects(Segment from, Segment to) throws
    IllegalArgumentException {
    return addEdge(from, to);
  }

  /**
   * Establish a composition relation between two segments in the graph.
   *
   * @param from the whole segment
   * @param to the part to be included in the whole segment.
   * @param cost the relation's weight/cost
   * @return true if the relation was established; false otherwise.
   * @throws IllegalArgumentException if from/to are not segments in the graph
   */
  default boolean connects(Segment from, Segment to, int cost) throws
    IllegalArgumentException {
    return addEdge(from, to, cost);
  }

  /**
   * Test if a segment is in the graph.
   *
   * @param v the segment to test.
   * @return true if the segment is in the graph; false otherwise.
   */
  default boolean contains(Segment v){
    return containsVertex(v);
  }

  /**
   * Tests if both segments are connected by checking if the whole segment
   * covers the part segment.
   *
   * @param whole the whole segment
   * @param part the part that should be included in the whole segment.
   * @return true if the whole segment covers the part segment; false otherwise.
   */
  default boolean covers(Segment whole, Segment part){
    return containsEdge(whole, part);
  }

  /**
   * Disconnects two segments in the graph.
   *
   * @param from the whole segment
   * @param to the part segment to be removed from the whole segment.
   * @return true if the segments are disconnected; false otherwise.
   */
  default boolean disconnects(Segment from, Segment to){
    return removeEdge(from, to);
  }

  /**
   * Returns the list of valid locations (i.e., locations we are interested in)
   * for targeted typicality and concept extraction.
   *
   * @param withinScope the current of scope provided by some user.
   * @return a new list of valid locations.
   */
  default List<Location> relevantLocations(Location withinScope) {
    final Set<Location> blackSet = irrelevantLocations(withinScope).stream().collect(Collectors.toSet());
    return getVertices().stream()
      .map(v -> ((Segment)v).getLocation())
      .filter(l -> !blackSet.contains(l))
      .collect(Collectors.toList());
  }


  /**
   * Returns the segment matching a given name.
   *
   * @param name the name of the segment.
   * @return the segment or null if the element
   *  is not in the segmentation graph.
   */
  default Segment segmentBy(String name){
    return Optional.ofNullable(getVertex(name))
      .map(v -> (Segment)v)
      .orElse(null);
  }

  /**
   * Returns the list of segments contained in this graph.
   *
   * @return a list of segments in the segmentation graph.
   */
  default List<Segment> segments(){
    return getVertices().stream()
      .map(v -> (Segment) v)
      .collect(Collectors.toList());
  }

  /**
   * Returns the root segment (if exists), otherwise returns a null
   * segment.
   *
   * @return the root segment.
   */
  default Segment rootSegment(){
    return Optional.ofNullable(getRootVertex())
      .map(v -> (Segment)v)
      .orElse(null);
  }

  /**
   * Removes a segment from the graph
   *
   * @param s the segment to remove
   * @return true if the segment was removed; false otherwise.
   */
  default boolean remove(Segment s){
    return removeVertex(s);
  }

  /**
   * Returns the non-informative (irrelevant to some capacity) segments
   * in this graph for the given capacity.
   *
   * @param capacity segmentation factor.
   * @return a list of segment locations.
   */
  List<Location> irrelevantLocations(int capacity);

  /**
   * Returns the non-informative (irrelevant to some capacity) segments
   * in this graph for the given scope.
   *
   * @param forScope the scope from where the capacity is inferred.
   *                 The capacity value is simply our segmentation factor.
   * @return a list of segment locations.
   */
  default List<Location> irrelevantLocations(Location forScope){
    return irrelevantLocations(
      (
        Math.abs(
          forScope.getEnd().getLine() - forScope.getStart().getLine()
        ) + 1
      )
    );
  }

  @Override String toString();
}
