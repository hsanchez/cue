package com.vesperin.cue.graph;

import com.vesperin.base.locations.Location;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * @author Huascar Sanchez
 */
public interface Segment extends Vertex <ASTNode> {
  /**
   * @return the location of the code segment
   * in the source file
   */
  Location location();

  /**
   * @return the segment's weight/cost
   */
  double weight();

  /**
   * @return the segment's benefit
   */
  double benefit();

  /**
   * @return the depth of this segment in a DAG.
   */
  int depth();
}
