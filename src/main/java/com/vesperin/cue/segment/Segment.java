package com.vesperin.cue.segment;

import com.vesperin.base.locations.Location;
import com.vesperin.cue.spi.Vertex;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * @author Huascar Sanchez
 */
interface Segment extends Vertex<ASTNode> {
  /**
   * @return the location of the code segment
   * in the source file
   */
  Location location();

  /**
   * @return the segment's benefit
   */
  double benefit();

  /**
   * @return the depth of this segment in a DAG.
   */
  int depth();
}
