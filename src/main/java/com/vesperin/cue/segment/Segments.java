package com.vesperin.cue.segment;

import com.vesperin.base.locations.Location;
import com.vesperin.base.locators.UnitLocation;

import java.util.Objects;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class Segments {
  private Segments(){}

  /**
   * Generates a new segmentation graph based on a located program unit..
   *
   * @param unitLocation located unit.
   * @return a new {@link SegmentationGraph segmentation} graph.
   */
  private static SegmentationGraph createSegmentationGraph(UnitLocation unitLocation){
    final BlockSegmentationVisitor visitor = new BlockSegmentationVisitor(unitLocation);

    unitLocation.getUnitNode().accept(visitor);

    return visitor.getBlockSegmentationGraph();
  }

  /**
   * Generates the set of irrelevant locations this introspector is not
   * interested in exploring.
   *
   * @param unitLocation the located unit of interest.
   * @return a new set of irrelevant locations.
   */
  public static Set<Location> irrelevantLocations(UnitLocation unitLocation){
    Objects.requireNonNull(unitLocation);
    final SegmentationGraph bsg = Segments.createSegmentationGraph(unitLocation);
    return bsg.irrelevantSet(unitLocation);
  }

  /**
   * Generates the set of relevant locations this introspector is interested in exploring.
   *
   * @param unitLocation the located unit of interest.
   * @return a new set of relevant locations.
   */
  public static Set<Location> relevantLocations(UnitLocation unitLocation){
    Objects.requireNonNull(unitLocation);
    final SegmentationGraph bsg = Segments.createSegmentationGraph(unitLocation);
    return bsg.relevantSet(unitLocation);
  }
}
