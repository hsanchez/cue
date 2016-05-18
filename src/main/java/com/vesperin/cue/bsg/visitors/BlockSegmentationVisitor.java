package com.vesperin.cue.bsg.visitors;

import com.google.common.base.Preconditions;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.utils.Jdt;
import com.vesperin.base.visitors.ASTVisitorWithHierarchicalWalk;
import com.vesperin.cue.bsg.AstUtils;
import com.vesperin.cue.bsg.BlockSegmentationGraph;
import com.vesperin.cue.bsg.CodeBlock;
import com.vesperin.cue.bsg.DirectedBlockSegmentationGraph;
import com.vesperin.cue.bsg.Segment;
import com.vesperin.cue.spi.GraphUtils;
import org.eclipse.jdt.core.dom.*;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * TODO delete after exhaustive testing,.. its replacement is SegmentationVisitor
 * @author Huascar Sanchez
 */
public class BlockSegmentationVisitor extends ASTVisitorWithHierarchicalWalk {
  private final Location scope;

  private final Set<ASTNode> outsiders;
  private final Set<ASTNode> visited;
  private final DirectedBlockSegmentationGraph dag = new DirectedBlockSegmentationGraph();

  /**
   * Scoped code segmentation step.
   *
   * @param scope the segmentation boundary.
   */
  public BlockSegmentationVisitor(Location scope){
    this.scope = Preconditions.checkNotNull(scope);
    this.outsiders = new HashSet<>();
    this.visited   = new HashSet<>();
  }


  @Override public boolean visit(Block node){
    if(Locations.covers(scope, Locations.locate(node)) || isOutsider(outsiders, node)){
      catchCodeBlock(node);
    }

    return super.visit(node);
  }

  @Override public boolean visit(TryStatement node) {
    if(Locations.covers(scope, Locations.locate(node)) || isOutsider(outsiders, node)){

      final ASTNode callingBlock = findFirstBlock(node);
      final Segment from = dag.segmentBy(callingBlock.toString());

      Segment to = CodeBlock.of(node);
      if(dag.contains(to)){
        to = dag.segmentBy(node.toString());
      } else {
        dag.addVertex(to);
      }

      if(!GraphUtils.isDescendantOf(from, to)){
        dag.connects(from, to);

        updateSegmentValues(from, to);
      }


      catchCodeBlock(node);
    }

    return super.visit(node);
  }

  @Override public boolean visit(MethodDeclaration node) {
    if(Locations.covers(scope, Locations.locate(node)) || isOutsider(outsiders, node)){
      catchCodeBlock(node);
    }

    return super.visit(node);
  }

  private void catchCodeBlock(ASTNode node){
    Segment from = CodeBlock.of(node);
    if(dag.contains(from)){
      from = dag.segmentBy(node.toString());
    }

    if(dag.rootSegment() == null){
      dag.addRoot(from);
    } else {
      dag.add(from);
    }

    final BlockVisitor statements = new BlockVisitor();
    if(!visited.contains(node)) node.accept(statements);


    final List<Block> children = statements.getCodeBlocks();
    for(Block each : children){
      if(visited.contains(each)) continue;

      visited.add(each);

      Segment to = CodeBlock.of(each);
      if(dag.contains(to)){
        to = dag.segmentBy(each.toString());
      } else {
        dag.add(to);
      }


      if(!GraphUtils.isDescendantOf(from, to)){
        dag.connects(from, to);

        updateSegmentValues(from, to);
      }

      each.accept(this);

    }
  }

  private void updateSegmentValues(Segment from, Segment to){

    Segment actualFrom = dag.segmentBy(from.getName());
    Segment actualTo   = dag.segmentBy(to.getName());

    final int depth = actualTo.getDepth();
    actualTo.setBenefit(actualTo.getBenefit() + calculateBenefit(actualTo.getData(), depth));

    // distribute weight among children
    if(isIncluded(actualFrom, actualTo)){ // is a segment (to) included in another segment (from)?
      final double weightChange = (actualFrom.getWeight() - actualTo.getWeight());
      actualFrom.setWeight((weightChange < 0.0 ? 0.0 : weightChange));
    }
  }

  private static boolean isIncluded(Segment whole, Segment part){
    return Locations.covers(locates(whole), locates(part));
  }

  private static Location locates(Segment segment){
    return Locations.locate(segment.getData());
  }

  private static boolean isOutsider(Set<ASTNode> outsiders, ASTNode node){

    for(ASTNode each : outsiders){
      if(Locations.covers(Locations.locate(each), Locations.locate(node))
        || outsiders.contains(node)) {
        return true;
      }
    }

    return false;
  }


  @Override public boolean visit(MethodInvocation node){
    if(Locations.covers(scope, Locations.locate(node))){

      final IMethodBinding methodBinding = node.resolveMethodBinding();
      final Optional<ASTNode> method = AstUtils.findASTDeclaration(methodBinding, node.getRoot());

      if(method.isPresent()){

        final ASTNode callingBlock = findFirstBlock(node);

        final MethodDeclaration declaration = (MethodDeclaration) method.get();
        final Segment from = dag.segmentBy(callingBlock.toString());

        final Segment to   = CodeBlock.of(declaration);
        dag.add(to);

        if(!GraphUtils.isDescendantOf(from, to)){
          dag.connects(from, to);

          updateSegmentValues(from, to);
        }

        if(!visited.contains(declaration)){
          outsiders.add(declaration);
          declaration.accept(this);
        }
      }

    }

    return super.visit(node);
  }

  @Override public boolean visit(SimpleType node){
    if(Locations.covers(scope, Locations.locate(node))){

      if(isTypeDeclarationStatement(node)){
        final Optional<ASTNode> declaration = AstUtils.findASTDeclaration(node.resolveBinding(), node);
        // todo fix this BAD BAD code
        final TypeDeclaration typeDeclaration = declaration.isPresent() ? (TypeDeclaration) declaration.get() : null;

        if(!Objects.isNull(typeDeclaration)){
          final BlockVisitor blocks = new BlockVisitor();
          typeDeclaration.accept(blocks);

          final List<Block> children = blocks.getCodeBlocks();

          if(!children.isEmpty()){
            outsiders.add(children.get(0));
            children.get(0).accept(this);
          } else {
            // Most likely this class contains only static fields.
            // Therefore, we can just consider its entire body declaration
            // as one segment. Moreover, we don't have to visit its declaration.
            if(!typeDeclaration.isPackageMemberTypeDeclaration()){
              final ASTNode callingBlock = findFirstBlock(node);
              final Segment from = dag.segmentBy(callingBlock.toString());

              final Segment to = CodeBlock.of(typeDeclaration);
              dag.add(to);

              if(!GraphUtils.isDescendantOf(from, to)){
                dag.connects(from, to);

                updateSegmentValues(from, to);
              }
            }
          }
        }

      }

    }

    return super.visit(node);
  }

  private static ASTNode findFirstBlock(ASTNode node){
    ASTNode parent = node.getParent();
    while(!(parent instanceof Block)){
      parent = parent.getParent();
    }

    return parent;
  }


  private static boolean isTypeDeclarationStatement(ASTNode node) {
    if (!SimpleType.class.isInstance(node)) return false;

    final SimpleType type = (SimpleType) node;

    final Optional<ASTNode> optionalDeclaration = AstUtils.findASTDeclaration(type.resolveBinding(), type);
    if(optionalDeclaration.isPresent()){
      final ASTNode declaration = optionalDeclaration.get();
      if (!TypeDeclaration.class.isInstance(declaration)) return false;

      final TypeDeclaration found = (TypeDeclaration) declaration;
      // TODO test whether we can handle anonymous class declarations
      return !found.isPackageMemberTypeDeclaration() || found.isMemberTypeDeclaration();

    }

    return false;
  }


  private static double calculateBenefit(ASTNode/*Block*/ node, int depth) {

    final CompilationUnit root = Jdt.parent(CompilationUnit.class, node);

    double b = 0;
    for(ASTNode each : Jdt.getChildren(node)){
      final ElementsVisitor visitor = new ElementsVisitor();
      each.accept(visitor);
      final Set<SimpleName> elements = visitor.getNodes();
      for(SimpleName eachName : elements){
        final double size = Math.abs(
          (AstUtils.findByNode(root, eachName).size()) - 1 /*declaration*/
        );

        b += (size / depth);
      }
    }

    return b;
  }

  public BlockSegmentationGraph getBlockSegmentationGraph(){
    return dag;
  }

}
