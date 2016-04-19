package com.vesperin.cue.bsg.visitors;

import com.google.common.base.Preconditions;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.utils.Jdt;
import com.vesperin.base.visitors.ASTVisitorWithHierarchicalWalk;
import com.vesperin.cue.bsg.CodeBlock;
import com.vesperin.cue.bsg.Segment;
import com.vesperin.cue.bsg.SegmentationGraph;
import com.vesperin.cue.spi.GraphUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
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
  private final BlockSegmentationGraph dag = new BlockSegmentationGraph();

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

      final CodeBlock from = CodeBlock.of(node);
      if(dag.getRootVertex() == null){
        dag.addRootVertex(from);
        visited.add(node);
      } else {
        dag.addVertex(from);
      }

      final BlockVisitor statements = new BlockVisitor();
      node.accept(statements);


      final List<Block> children = statements.getCodeBlocks();
      for(ASTNode each : children){
        if(visited.contains(each)) continue;

        visited.add(each);

        final CodeBlock to = CodeBlock.of(each);
        dag.addVertex(to);

        if(!GraphUtils.isDescendantOf(from, to)){
          dag.addEdge(from, to);

          updateSegmentValues(from, to);
        }

        each.accept(this);

      }

    }

    return super.visit(node);
  }


  private static void updateSegmentValues(Segment from, Segment to){
    final int depth = to.getDepth();
    to.setBenefit(to.getBenefit() + calculateBenefit(to.getData(), depth));

    // distribute weight among children
    if(isIncluded(from, to)){ // is a segment (to) included in another segment (from)?
      final double weightChange = (from.getWeight() - to.getWeight());
      from.setWeight((weightChange < 0.0 ? 0.0 : weightChange));
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
        final BlockVisitor blocks = new BlockVisitor();
        method.get().accept(blocks);

        final List<Block> children = blocks.getCodeBlocks();

        if(!children.isEmpty()){
          outsiders.add(children.get(0));
          children.get(0).accept(this);
        }
      }

    }

    return super.visit(node);
  }

  @Override public boolean visit(SimpleType node){
    if(Locations.covers(scope, Locations.locate(node))){

      if(isTypeDeclarationStatement(node)){
        final Optional<ASTNode> declaration = AstUtils.findASTDeclaration(node.resolveBinding(), node);
        // todo fix this ugly code
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
              final Segment from = (Segment) dag.getVertex(callingBlock.toString());

              final CodeBlock to   = CodeBlock.of(typeDeclaration);
              dag.addVertex(to);

              if(!GraphUtils.isDescendantOf(from, to)){
                dag.addEdge(from, to);

                updateSegmentValues(from, to);
              }
            }
          }
        }

      }

    }

    return super.visit(node);
  }

  private static ASTNode findFirstBlock(SimpleType node){
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
      // todo(Huascar) test whether we can handle anonymous class declarations
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

        b += size / depth;
      }
    }

    return b;
  }

  public SegmentationGraph getBSG(){
    return dag;
  }

}
