package com.vesperin.cue.segment;

import com.google.common.base.Preconditions;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.utils.Jdt;
import com.vesperin.base.visitors.ASTVisitorWithHierarchicalWalk;
import com.vesperin.cue.utils.AstUtils;
import org.eclipse.jdt.core.dom.*;

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
  private final BlockSegmentationGraph dag;

  /**
   * Scoped code segmentation step.
   *
   * @param scope the segmentation boundary.
   */
  public BlockSegmentationVisitor(Location scope){
    this.scope = Preconditions.checkNotNull(scope);
    this.outsiders = new HashSet<>();
    this.visited   = new HashSet<>();
    this.dag       = new BlockSegmentationGraph();
  }


  @Override public boolean visit(Block node){
    if(Locations.covers(scope, Locations.locate(node)) || isOutsider(outsiders, node)){

      final ASTNode callingBlock = findParentBlock(node);

      if(callingBlock != null){
        final Segment from = dag.segmentBy(callingBlock.toString());

        Segment to = CodeSegment.of(node);
        if(dag.containsVertex(to)){
          to = dag.segmentBy(node.toString());
        } else {
          dag.addVertex(to);
        }

        boolean seenAndDoneBefore = visited.contains(callingBlock) && visited.contains(node);

        if(!seenAndDoneBefore && !dag.isDescendantOf(from, to)){

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

        final boolean isAbstract = Modifier.isAbstract(node.getModifiers());
        if(isAbstract) return false;

        final Optional<ASTNode> firstBlock = findFirstBlock(node);
        if(!firstBlock.isPresent() ){
          System.out.println("Found a method declaration with no body");
          return false;
        }

        final ASTNode firstBlockNode = firstBlock.get();

        if(dag.getRootVertex() == null){
          dag.addRootVertex(CodeSegment.of(node));
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
          if(!Objects.isNull(parentBlock)){
            final BlockVisitor blockVisitor = new BlockVisitor();
            node.accept(blockVisitor);

            for(Block each : blockVisitor.getCodeBlocks()){
              linkNodes(parentBlock, each);

              catchCodeBlock(each);
            }
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
    if(dag.containsVertex(CodeSegment.of(callingBlock))){
      from = dag.segmentBy(callingBlock.toString());
    } else {
      from = CodeSegment.of(callingBlock);
      dag.addVertex(from);
    }

    final Segment to = CodeSegment.of(calledBlock);
    dag.addVertex(to);

    if(!dag.isDescendantOf(from, to)){
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
    Segment from = CodeSegment.of(node);
    if(dag.containsVertex(from)){
      from = dag.segmentBy(node.toString());
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

      Segment to = CodeSegment.of(each);
      if(dag.containsVertex(to)){
        to = dag.segmentBy(each.toString());
      } else {
        dag.addVertex(to);
      }


      if(!dag.isDescendantOf(from, to)){
        dag.addEdge(from, to);

        updateSegmentValues(dag, from, to);
      }

      each.accept(this);

    }
  }

  private static void updateSegmentValues(SegmentationGraph dag, Segment from, Segment to){

    Segment actualFrom = dag.segmentBy(Objects.requireNonNull(from).label());
    Segment actualTo   = dag.segmentBy(Objects.requireNonNull(to).label());

    final int depth = actualTo.depth();
    final CodeSegment castActualTo = ((CodeSegment) actualTo);
    castActualTo.updateBenefit(actualTo.benefit() + calculateBenefit(actualTo.data(), depth));

    // distribute weight among children
    if(isIncluded(actualFrom, actualTo)){ // is a segment (to) included in another segment (from)?
      final CodeSegment castActualFrom = ((CodeSegment) actualTo);
      final double weightChange = (actualFrom.weight() - actualTo.weight());
      castActualFrom.updateWeight((weightChange < 0.0 ? 0.0 : weightChange));
    }
  }

  private static boolean isIncluded(Segment whole, Segment part){
    return Locations.covers(locates(whole), locates(part));
  }

  private static Location locates(Segment segment){
    return Locations.locate(segment.data());
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

  public SegmentationGraph getBlockSegmentationGraph(){
    return dag;
  }
}
