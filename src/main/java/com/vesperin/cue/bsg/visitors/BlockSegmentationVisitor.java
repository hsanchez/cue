package com.vesperin.cue.bsg.visitors;

import com.google.common.base.Preconditions;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.utils.Jdt;
import com.vesperin.base.visitors.ASTVisitorWithHierarchicalWalk;
import com.vesperin.cue.bsg.AstUtils;
import com.vesperin.cue.bsg.DirectedBlockSegmentationGraph;
import com.vesperin.cue.bsg.CodeBlock;
import com.vesperin.cue.bsg.Segment;
import com.vesperin.cue.bsg.BlockSegmentationGraph;
import com.vesperin.cue.spi.GraphUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class BlockSegmentationVisitor extends ASTVisitorWithHierarchicalWalk {
  private final Location scope;

  private final Set<ASTNode> outsiders;
  private final Set<ASTNode> visited;
  private final DirectedBlockSegmentationGraph dag;

  /**
   * Scoped code segmentation step.
   *
   * @param scope the segmentation boundary.
   */
  public BlockSegmentationVisitor(Location scope){
    this.scope = Preconditions.checkNotNull(scope);
    this.outsiders = new HashSet<>();
    this.visited   = new HashSet<>();
    this.dag       = new DirectedBlockSegmentationGraph();
  }


  @Override public boolean visit(Block node){
    if(Locations.covers(scope, Locations.locate(node)) || isOutsider(outsiders, node)){

      final ASTNode callingBlock = findParentBlock(node);

      if(callingBlock != null){
        final Segment from = (Segment) dag.getVertex(callingBlock.toString());

        CodeBlock to = CodeBlock.of(node);
        if(dag.containsVertex(to)){
          to = (CodeBlock) dag.getVertex(node.toString());
        } else {
          dag.addVertex(to);
        }

        boolean seenAndDoneBefore = visited.contains(callingBlock) && visited.contains(node);

        if(!seenAndDoneBefore && !GraphUtils.isDescendantOf(from, to)){
          dag.addEdge(from, to);

          updateSegmentValues(dag, from, to);
        }
      }

      catchCodeBlock(node);
    }

    return super.visit(node);
  }

  @Override public boolean visit(MethodDeclaration node) {
    if(Locations.covers(scope, Locations.locate(node)) || isOutsider(outsiders, node)){
      final ASTNode parentBlock = findParentBlock(node);
      if(parentBlock == null){

        final Optional<ASTNode> firstBlock = findFirstBlock(node);
        if(!firstBlock.isPresent()){
          throw new IllegalArgumentException("A method declaration must have a block statement");
        }

        final ASTNode firstBlockNode = firstBlock.get();

        if(dag.getRootVertex() == null){
          dag.addRootVertex(CodeBlock.of(node));
        }

        catchFirstCodeBlock(node, firstBlockNode);

      } else {
        catchFirstCodeBlock(parentBlock, node);
      }

    }

    return super.visit(node);
  }


  @Override public boolean visit(MethodInvocation node){
    if(Locations.covers(scope, Locations.locate(node))){
      final IMethodBinding methodBinding = node.resolveMethodBinding();
      if(!Objects.isNull(methodBinding)){
        // resolves method declaration
        final Optional<ASTNode> optionalMethodDeclare = AstUtils.findASTDeclaration(
          methodBinding,
          node.getRoot()
        );

        if(optionalMethodDeclare.isPresent()){
          final ASTNode parentBlock = findParentBlock(node);
          catchFirstCodeBlock(parentBlock, optionalMethodDeclare.get());
        }
      }
    }

    return super.visit(node);
  }


  @Override public boolean visit(SimpleType node){
    if(Locations.covers(scope, Locations.locate(node))){

      if(isTypeDeclarationStatement(node)){

        final Optional<ASTNode> declaration   = AstUtils.findASTDeclaration(
          node.resolveBinding(), node
        );

        final TypeDeclaration typeDeclaration = (declaration.isPresent()
          ? (TypeDeclaration) declaration.get()
          : null
        );

        if(!Objects.isNull(typeDeclaration)){

          final ASTNode parentBlock = findParentBlock(node);

          Objects.requireNonNull(parentBlock);

          final BlockVisitor blockVisitor = new BlockVisitor();
          node.accept(blockVisitor);

          for(Block each : blockVisitor.getCodeBlocks()){
            linkNodes(parentBlock, each);

            catchCodeBlock(each);
          }
        }
      }

    }

    return super.visit(node);
  }


  private static boolean isTypeDeclarationStatement(ASTNode node) {
    if (!(node instanceof SimpleType)) return false;

    final SimpleType type = (SimpleType) node;

    final Optional<ASTNode> optionalDeclaration = AstUtils.findASTDeclaration(type.resolveBinding(), type);
    if(optionalDeclaration.isPresent()){
      final ASTNode declaration = optionalDeclaration.get();
      if (!(declaration instanceof TypeDeclaration)) return false;

      final TypeDeclaration found = (TypeDeclaration) declaration;
      // todo(Huascar) test whether we can handle anonymous class declarations
      return !found.isPackageMemberTypeDeclaration() || found.isMemberTypeDeclaration();

    }

    return false;
  }



  private void catchFirstCodeBlock(ASTNode callingBlock, ASTNode node) {
    final Optional<ASTNode> optional = findFirstBlock(node);

    if(optional.isPresent()){

      final ASTNode calledBlock  = optional.get();

      if(callingBlock != null){
        // links both nodes
        linkNodes(callingBlock, calledBlock);
      }

      // crawls the called block and search for its block children
      catchCodeBlock(calledBlock);
    }
  }

  private static Optional<ASTNode> findFirstBlock(ASTNode node){

    if(node instanceof Block) return Optional.of(node);

    return Jdt.getChildren(node).stream()
      .filter(n -> (n instanceof Block))
      .findFirst();
  }

  private void linkNodes(ASTNode callingBlock, ASTNode calledBlock){
    if(calledBlock == callingBlock) return;
    Segment from;
    if(dag.containsVertex(CodeBlock.of(callingBlock))){
      from = (Segment) dag.getVertex(callingBlock.toString());
    } else {
      from = CodeBlock.of(callingBlock);
      dag.addVertex(from);
    }

    final CodeBlock to   = CodeBlock.of(calledBlock);
    dag.addVertex(to);

    if(!GraphUtils.isDescendantOf(from, to)){
      dag.addEdge(from, to);

      updateSegmentValues(dag, from, to);
    }

    if(!isIncluded(from, to)){
      if(!visited.contains(calledBlock)){
        outsiders.add(calledBlock);
        calledBlock.accept(this);
      }
    }


  }

  private static ASTNode findParentBlock(ASTNode node){
    ASTNode parent = node.getParent();
    while(parent != null && !(parent instanceof Block)){
      parent = parent.getParent();
    }

    return parent;
  }

  private void catchCodeBlock(ASTNode node){
    CodeBlock from = CodeBlock.of(node);
    if(dag.containsVertex(from)){
      from = (CodeBlock) dag.getVertex(node.toString());
    } else {
      dag.addVertex(from);
    }

    if(dag.getRootVertex() == null){
      dag.addRootVertex(from);
    }

    final BlockVisitor statements = new BlockVisitor();
    if(!visited.contains(node)) node.accept(statements);

    final List<Block> children = statements.getCodeBlocks();

    for(Block each : children){
      if(visited.contains(each)) continue;
      if(each == node) {
        visited.add(each);
        continue;
      }

      visited.add(each);

      CodeBlock to = CodeBlock.of(each);
      if(dag.containsVertex(to)){
        to = (CodeBlock) dag.getVertex(each.toString());
      } else {
        dag.addVertex(to);
      }


      if(!GraphUtils.isDescendantOf(from, to)){
        dag.addEdge(from, to);

        updateSegmentValues(dag, from, to);
      }

      each.accept(this);

    }
  }

  private static void updateSegmentValues(DirectedBlockSegmentationGraph dag, Segment from, Segment to){

    Segment actualFrom = castVertex(dag, from);
    Segment actualTo   = castVertex(dag, to);

    final int depth = actualTo.getDepth();
    actualTo.setBenefit(actualTo.getBenefit() + calculateBenefit(actualTo.getData(), depth));

    // distribute weight among children
    if(isIncluded(actualFrom, actualTo)){ // is a segment (to) included in another segment (from)?
      final double weightChange = (actualFrom.getWeight() - actualTo.getWeight());
      actualFrom.setWeight((weightChange < 0.0 ? 0.0 : weightChange));
    }
  }

  private static Segment castVertex(DirectedBlockSegmentationGraph dag, Segment other){
    return (Segment) dag.getVertex(Objects.requireNonNull(other).getName());
  }

  private static boolean isIncluded(Segment whole, Segment part){
    return Locations.covers(locates(whole), locates(part));
  }

  private static Location locates(Segment segment){
    return Locations.locate(segment.getData());
  }

  private static double calculateBenefit(ASTNode/*Block*/ node, int depth) {

    final CompilationUnit root = Jdt.parent(CompilationUnit.class, node);

    double b = 0;
    final List<ASTNode> children = Jdt.getChildren(node);
    for(ASTNode each : children){
      final ElementsVisitor visitor = new ElementsVisitor();
      each.accept(visitor);
      final Set<SimpleName> elements = visitor.getNodes();
      for(SimpleName eachName : elements){
        final double size = Math.abs(
          (AstUtils.findByNode(root, eachName).size()) - 1 /*declaration*/
        );

        b += size / depth;
      }
    }

    return b;
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

  public BlockSegmentationGraph getBlockSegmentationGraph(){
    return dag;
  }
}
