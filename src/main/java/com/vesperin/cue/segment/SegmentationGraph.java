package com.vesperin.cue.segment;

import com.google.common.collect.Sets;
import com.vesperin.base.locations.Location;
import com.vesperin.cue.spi.DirectedAcyclicGraph;
import com.vesperin.cue.spi.Edge;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public interface SegmentationGraph extends DirectedAcyclicGraph<Segment, Edge<Segment>> {

  /**
   * Returns the segment matching a label.
   *
   * @param label the matching label
   * @return the matched segment.
   */
  Segment segmentBy(String label);

  /**
   * Check whether a child segment is a descendant (immediate or distant) of a
   * parent segment.
   *
   * @param child  The child segment
   * @param parent The parent segment
   * @return true if the child is a descendant of the parent; false otherwise.
   */
  default boolean isDescendantOf(Segment child, Segment parent) {

    final Deque<Segment> stack = new LinkedList<>();
    if(child != null) stack.push(child);

    final Set<Segment> visited = Sets.newLinkedHashSet();

    while (!stack.isEmpty()) {
      final Segment current = stack.pop();
      visited.add(current);

      for( Edge<Segment> each : incomingEdgesOf(current)){
        if(Objects.equals(each.from(), parent)) return true;
        if(!visited.contains(each.from())){
          visited.add(each.from());
        }
      }
    }

    return false;
  }

  /**
   * Gets a segment at a nth position in the graph.
   *
   * @param n the index [0, size()-1] of the Vertex to access
   * @return the segment at that nth position
   */
  Segment segmentAt(int n);

  /**
   * Returns the list of valid locations (i.e., locations we are interested in)
   * for targeted typicality and concept extraction.
   *
   * @param withinScope the current of scope provided by some user.
   * @return a new list of valid locations.
   */
  default Set<Location> relevantSet(Location withinScope) {
    final Set<Location> blackSet = irrelevantSet(withinScope).stream()
      .collect(Collectors.toSet());

    final Set<Location> universe = segmentSet().stream()
      .map(Segment::location)
      .collect(Collectors.toSet());

    return Sets.difference(universe, blackSet)
      .immutableCopy();
  }

  /**
   * Returns the non-informative (irrelevant to some capacity) segments
   * in this graph for the given capacity.
   *
   * @param capacity segmentation factor.
   * @return a list of segment locations.
   */
  List<Location> irrelevantSet(int capacity);

  /**
   * Returns the non-informative (irrelevant to some capacity) segments
   * in this graph for the given scope.
   *
   * @param forScope the scope from where the capacity is inferred.
   *                 The capacity value is simply our segmentation factor.
   * @return a list of segment locations.
   */
  default List<Location> irrelevantSet(Location forScope){
    return irrelevantSet(
      (
        Math.abs(
          forScope.getEnd().getLine() - forScope.getStart().getLine()
        ) + 1
      )
    );
  }

  /**
   * Returns the list of segments contained in this graph.
   *
   * @return a list of segments in the segmentation graph.
   */
  default Set<Segment> segmentSet(){
    return vertexSet().stream()
      .map(v -> v)
      .collect(Collectors.toSet());
  }

  @Override String toString();
}
