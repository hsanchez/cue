package com.vesperin.cue.bsg;

import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.cue.spi.Edge;
import com.vesperin.cue.spi.Vertex;
import com.vesperin.cue.spi.VertexImpl;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.List;
import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
public class CodeSegment implements Segment {

  private double benefit;
  private double weight;

  private final int             depth;
  private final Vertex<ASTNode> impl;

  private final Location        location;

  /**
   * Construct a segment node for a given value.
   *
   * @param value the ASTNode object.
   */
  private CodeSegment(ASTNode value){
    this(
      new VertexImpl<>(
        Objects.requireNonNull(value).toString(),
        value
      ), 1
    );
  }

  /**
   * Construct a segment node
   *
   * @param impl delegating implementation.
   * @param benefit benefit of this segment.
   */
  private CodeSegment(Vertex<ASTNode> impl, int benefit){
    this.impl     = impl;
    this.benefit  = benefit;
    this.weight   = calculateNumberOfLines(getData());
    this.depth    = calculateDepth(getData());

    this.location = Locations.locate(impl.getData());
  }

  /**
   * Create a new code block for a given Block AST Node.
   *
   * @param block the block ast node.
   * @return a new CodeSegment object.
   */
  public static CodeSegment of(ASTNode block){
    return new CodeSegment(block);
  }

  private static double calculateNumberOfLines(ASTNode node) {
    final Location location = Locations.locate(node);

    return (Math.abs(location.getEnd().getLine() - location.getStart().getLine()) + 1)/*inclusive*/;
  }

  private static int calculateDepth(ASTNode node){
    ASTNode parent = node;
    int depth = 0;
    do {
      parent = parent.getParent();
      if (parent != null) {
        depth ++;
      }
    } while (parent != null);

    return depth;
  }

  @Override public boolean equals(Object obj) {
    return Segment.class.isInstance(obj) && impl.equals(obj);
  }

  @Override public String getName() {
    return impl.getName();
  }

  @Override public Location getLocation() {
    return location;
  }

  @Override public double getBenefit() {
    return benefit;
  }

  @Override public int getDepth() {
    return depth;
  }

  @Override public ASTNode getData() {
    return this.impl.getData();
  }

  @Override public List<Edge<ASTNode>> getIncomingEdges() {
    return impl.getIncomingEdges();
  }

  @Override public int getMarkState() {
    return 0;
  }

  @Override public List<Edge<ASTNode>> getOutgoingEdges() {
    return impl.getOutgoingEdges();
  }

  @Override public double getWeight(){
    return weight;
  }

  @Override public int hashCode() {
    return impl.hashCode();
  }

  @Override public void setData(ASTNode data) {
    this.impl.setData(data);
  }

  @Override public void setWeight(double weight){
    this.weight = weight;
  }

  @Override public void setBenefit(double benefit){
    this.benefit = benefit;
  }

  @Override public void setMarkState(int state) {

  }
}
