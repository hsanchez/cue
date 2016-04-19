package com.vesperin.cue.bsg;

import com.vesperin.base.locations.Location;
import com.vesperin.cue.spi.Vertex;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Reified version of {@link Vertex} type.
 *
 * @author Huascar Sanchez
 */
public interface Segment extends Vertex <ASTNode> {

  /**
   * @return the location of {@link #getData()} in some AST.
   */
  Location getLocation();

  /**
   * @return the benefit value of this segment.
   */
  double getBenefit();

  /**
   * @return the weight value of this segment.
   */
  double getWeight();

  /**
   * @return the depth of this segment in a DAG.
   */
  int getDepth();

  /**
   * Sets a new weight value for this segment.
   *
   * @param weight the new weight value.
   */
  void setWeight(double weight);

  /**
   * Sets a new benefit value for this segment.
   *
   * @param benefit the new benefit value.
   */
  void setBenefit(double benefit);

}
