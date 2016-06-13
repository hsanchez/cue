package com.vesperin.cue.segment;

import com.google.common.base.Preconditions;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
class CodeSegment extends AbstractSegment implements Segment {


  private double weight;
  private double benefit;

  private int depth;

  /**
   * Construct a segment node for a given value.
   *
   * @param data the ASTNode object.
   */
  CodeSegment(ASTNode data){
    this(
      Objects.requireNonNull(data),
      calculateNumberOfLines(data),
      1.0
    );
  }

  /**
   * Construct a segment node for a given set of values.
   *
   * @param data the AST node
   * @param weight the weight of this node
   * @param benefit the benefit of using this node.
   */
  private CodeSegment(ASTNode data, double weight, double benefit){
    super(data);

    this.weight   = weight;
    this.benefit  = benefit;
    this.depth    = calculateDepth(data);
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

  @Override public void updateWeight(double weight) {
    Preconditions.checkArgument(weight >= 0.0);
    this.weight = weight;
  }

  @Override public void updateBenefit(double benefit) {
    Preconditions.checkArgument(benefit >= 0.0);
    this.benefit = benefit;
  }

  @Override public boolean equals(Object obj) {
    return Segment.class.isInstance(obj) && Objects.equals(data(), ((Segment)obj).data());
  }

  @Override public int hashCode() {
    return data().hashCode();
  }

  @Override public double weight() {
    return weight;
  }

  @Override public double benefit() {
    return benefit;
  }

  @Override public int depth() {
    return depth;
  }
}
