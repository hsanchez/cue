package com.vesperin.cue.utils;

import com.google.common.collect.ImmutableList;
import com.vesperin.cue.segment.LabelVisitor;
import com.vesperin.cue.segment.LinkedNodesVisitor;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Huascar Sanchez
 */
public class AstUtils {
  /**
   * Private constructor for AstUtils.
   */
  private AstUtils(){
    throw new Error("Cannot be instantiated");
  }

  private static final int FIELD  = 1;
  private static final int METHOD = 2;
  private static final int TYPE   = 4;
  private static final int LABEL  = 8;
  private static final int NAME   = FIELD | TYPE;


  public static Optional<ASTNode> findASTDeclaration(IBinding binding, ASTNode node){
    final ASTNode root = node.getRoot();
    if(Objects.isNull(root) || !(root instanceof CompilationUnit)) return Optional.empty();

    return Optional.ofNullable(((CompilationUnit) root).findDeclaringNode(binding));
  }

  /**
   * Get all nodes connected to the given name node. If the node has a binding then all nodes connected
   * to this binding are returned. If the node has no binding, then all nodes that also miss a binding
   * and have the same name are returned.
   *
   * @param root The root of the AST tree to search
   * @param name The node to find linked nodes for
   * @return The list of all nodes that have the same name or are connected to
   *      name's binding (if binding is available)
   */
  public static List<SimpleName> findByNode(ASTNode root, SimpleName name) {
    final IBinding binding = name.resolveBinding();

    if (binding != null) {
      return findByBinding(root, binding);
    }

    final List<SimpleName> names = findByProblems(root, name);

    if (names != null) {
      return names;
    }

    int parentKind = name.getParent().getNodeType();
    if (parentKind == ASTNode.LABELED_STATEMENT
      || parentKind == ASTNode.BREAK_STATEMENT
      || parentKind == ASTNode.CONTINUE_STATEMENT) {

      final LabelVisitor labelVisitor = new LabelVisitor(name);

      root.accept(labelVisitor);

      return labelVisitor.getLabels();
    }

    return ImmutableList.of(name);
  }

  private static List<SimpleName> findByProblems(ASTNode parent, SimpleName nameNode) {
    final List<SimpleName> result = new ArrayList<>();

    final ASTNode astRoot = parent.getRoot();

    if (!(astRoot instanceof CompilationUnit)) {
      return ImmutableList.of();
    }

    final IProblem[] problems = ((CompilationUnit)astRoot).getProblems();

    int nameNodeKind = getNameNodeProblemKind(problems, nameNode);
    if (nameNodeKind == 0) { // no problem on node
      return ImmutableList.of();
    }

    int bodyStart   = parent.getStartPosition();
    int bodyEnd     = bodyStart + parent.getLength();

    String name = nameNode.getIdentifier();

    for (IProblem each : problems) {
      int probStart   = each.getSourceStart();
      int probEnd     = each.getSourceEnd() + 1;

      if (probStart > bodyStart && probEnd < bodyEnd) {
        int currKind = getProblemKind(each);
        if ((nameNodeKind & currKind) != 0) {
          ASTNode node = NodeFinder.perform(parent, probStart, (probEnd - probStart));
          if (node instanceof SimpleName
            && name.equals(((SimpleName)node).getIdentifier())) {
            result.add((SimpleName) node);
          }
        }
      }
    }

    return result;
  }


  private static int getProblemKind(IProblem problem) {
    switch (problem.getID()) {
      case IProblem.UndefinedField:
        return FIELD;
      case IProblem.UndefinedMethod:
        return METHOD;
      case IProblem.UndefinedLabel:
        return LABEL;
      case IProblem.UndefinedName:
      case IProblem.UnresolvedVariable:
        return NAME;
      case IProblem.UndefinedType:
        return TYPE;
    }
    return 0;
  }

  /**
   * Get all the AST nodes connected to a given binding. e.g. Declaration of a field and all
   * references. For types, this includes also the constructor declaration. For methods also
   * overridden methods or methods overriding (if existing in the same AST)
   *
   * @param root The root of the AST tree to search; e.g., Type declaration, Method declaration..
   * @param binding The binding of the searched nodes.
   * @return The list of nodes linked to a binding; an empty list if there are none.
   */
  private static List<SimpleName> findByBinding(ASTNode root, IBinding binding) {
    final LinkedNodesVisitor linkedBindings = new LinkedNodesVisitor(binding);
    root.accept(linkedBindings);

    return linkedBindings.getLinkedNodes();
  }

  /**
   * Downcast a method node to its method declaration.
   *
   * @param node node to downcast.
   * @return a method declaration
   */
  public static MethodDeclaration methodDeclaration(ASTNode node){
    assert node.getNodeType() == ASTNode.METHOD_DECLARATION;

    return ((MethodDeclaration) node);
  }


  private static int getNameNodeProblemKind(IProblem[] problems, SimpleName nameNode) {
    final int nameOffset  = nameNode.getStartPosition();
    final int nameInclEnd = nameOffset + nameNode.getLength() - 1;

    for (IProblem each : problems) {
      if (each.getSourceStart() == nameOffset && each.getSourceEnd() == nameInclEnd) {
        int kind = getProblemKind(each);
        if (kind != 0) {
          return kind;
        }
      }
    }

    return 0;
  }
}
