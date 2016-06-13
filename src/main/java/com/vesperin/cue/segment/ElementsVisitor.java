package com.vesperin.cue.segment;

import com.vesperin.base.utils.Jdt;
import com.vesperin.base.visitors.SkeletalVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class ElementsVisitor extends SkeletalVisitor {
  private final Set<SimpleName> nodes;
  private final Set<ASTNode>    visited;

  public ElementsVisitor(){
    this.nodes    = new HashSet<>();
    this.visited  = new HashSet<>();
  }


  public Set<SimpleName> getNodes(){
    return nodes;
  }

  private boolean contains(ASTNode node){
    return visited.contains(node);
  }

  @Override public boolean visit(FieldAccess node) {
    nodes.add(node.getName());
    return false;
  }

  @Override public boolean visit(ArrayAccess node) {
    if(!contains(node)){
      visited.add(node);
      node.accept(this);
    }
    return false;
  }

  @Override public boolean visit(MethodInvocation node) {
    nodes.add(node.getName());

    final List<ASTNode> arguments = Jdt.typeSafeList(ASTNode.class, node.arguments());

    for(ASTNode each : arguments){
      if(contains(each)) continue;

      visited.add(each);
      each.accept(this);
    }

    return false;
  }

  @Override public boolean visit(SuperFieldAccess node) {
    nodes.add(node.getName());
    return false;
  }

  @Override public boolean visit(SuperMethodInvocation node) {
    nodes.add(node.getName());
    return false;
  }


  @Override public boolean visit(SimpleName node) {
    nodes.add(node);
    return false;
  }

  @Override public boolean visit(LabeledStatement node) {
    nodes.add(node.getLabel());
    return false;
  }
}
