package com.vesperin.cue.segment;

import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
abstract class AbstractSegment implements Segment {

  private final String label;
  private final ASTNode  data;
  private final Location location;
  private int markState;

  /**
   * Constructs a new AbstractSegment
   *
   * @param data Node in a AST.
   */
  AbstractSegment(ASTNode data){
    this(Objects.requireNonNull(data).toString(), data);
  }

  /**
   * Constructs a new AbstractSegment
   *
   * @param data Node in a AST.
   */
  private AbstractSegment(String label, ASTNode data){
    this(label, Objects.requireNonNull(data), Locations.locate(data));
  }

  /**
   * Constructs a new AbstractSegment
   *
   * @param data Node in a AST.
   * @param location the location of the node in the source file.
   */
  private AbstractSegment(String label, ASTNode data, Location location){
    this.label      = label;
    this.data       = data;
    this.location   = location;
    this.markState  = -1;
  }

  /**
   * Updates the segment's weight
   *
   * @param weight segment's weight
   */
  public abstract void updateWeight(double weight);

  /**
   * Updates the segment's benefit
   * @param benefit segment's benefit
   */
  public abstract void updateBenefit(double benefit);

  @Override public String label() {
    return label;
  }

  @Override public Location location() {
    return location;
  }

  @Override public ASTNode data() {
    return data;
  }

  @Override public int markState() {
    return markState;
  }

  @Override public void setMarkState(int state) {
    if(state < 0) throw new IllegalArgumentException("Negative integers are not allowed");
    this.markState = state;
  }
}
