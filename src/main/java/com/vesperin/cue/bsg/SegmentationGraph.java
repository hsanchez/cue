package com.vesperin.cue.bsg;

import com.vesperin.base.locations.Location;
import com.vesperin.cue.spi.DirectedGraph;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Reified version for DirectedGraph type -- using segments.
 *
 * @author Huascar Sanchez
 */
public interface SegmentationGraph extends DirectedGraph <ASTNode> {

  /**
   * Returns the list of valid locations (i.e., locations we are interested in)
   * for targeted typicality and concept extraction.
   *
   * @param scope the current of scope provided by some user.
   * @return a new list of valid locations.
   */
  default List<Location> whitelist(Location scope) {
    final Set<Location> blackSet = blacklist(scope).stream().collect(Collectors.toSet());
    return getVertices().stream()
      .map(v -> ((Segment)v).getLocation())
      .filter(l -> !blackSet.contains(l))
      .collect(Collectors.toList());
  }

  /**
   * Returns the non-informative (irrelevant to some capacity) segments
   * in this graph for the given capacity.
   *
   * @param capacity segmentation factor.
   * @return a list of segment locations.
   */
  List<Location> blacklist(int capacity);

  /**
   * Returns the non-informative (irrelevant to some capacity) segments
   * in this graph for the given scope.
   *
   * @param forScope the scope from where the capacity is inferred.
   *                 The capacity value is simply our segmentation factor.
   * @return a list of segment locations.
   */
  default List<Location> blacklist(Location forScope){
    return blacklist(
      (
        Math.abs(
          forScope.getEnd().getLine() - forScope.getStart().getLine()
        ) + 1
      )
    );
  }

  @Override String toString();
}
