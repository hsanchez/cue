package com.vesperin.cue.bsg;

import com.vesperin.base.locations.Location;
import com.vesperin.cue.spi.DirectedGraph;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.List;


/**
 * Reified version for DirectedGraph type -- using segments.
 *
 * @author Huascar Sanchez
 */
public interface SegmentationGraph extends DirectedGraph <ASTNode> {
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
