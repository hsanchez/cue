package com.vesperin.cue.bsg.visitors;

import com.vesperin.base.utils.Jdt;
import com.vesperin.base.visitors.SkeletalVisitor;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class BlockVisitor extends SkeletalVisitor {
  private List<Block> blocks;
  private Set<Block>  visited;

  /**
   * Construct a Statement Iterator.
   */
  public BlockVisitor(){
    blocks = new ArrayList<>();
    visited    = new HashSet<>();
  }

  @Override public boolean visit(Block node){
    if(!visited.contains(node)){
      blocks.add(node);
      visited.add(node);

      for(ASTNode eachChild : Jdt.getChildren(node)){
        eachChild.accept(this);
      }
    }
    return super.visit(node);
  }

  @Override public boolean visit(DoStatement node){
    final Optional<Block> firstBlock = findFirstBlock(node);
    if(firstBlock.isPresent()){
      firstBlock.get().accept(this);
    }
    return super.visit(node);
  }

  @Override public boolean visit(EnhancedForStatement node){
    final Optional<Block> firstBlock = findFirstBlock(node);
    if(firstBlock.isPresent()){
      firstBlock.get().accept(this);
    }
    return super.visit(node);
  }

  @Override public boolean visit(ForStatement node){
    final Optional<Block> firstBlock = findFirstBlock(node);
    if(firstBlock.isPresent()){
      firstBlock.get().accept(this);
    }
    return super.visit(node);
  }

  @Override public boolean visit(IfStatement node){
    final Optional<Block> firstBlock = findFirstBlock(node);
    if(firstBlock.isPresent()){
      firstBlock.get().accept(this);
    }
    return false;
  }

  @Override public boolean visit(SwitchCase node){
    final Optional<Block> firstBlock = findFirstBlock(node);
    if(firstBlock.isPresent()){
      firstBlock.get().accept(this);
    }
    return super.visit(node);
  }

  @Override public boolean visit(TryStatement node){
    final Optional<Block> firstBlock = findFirstBlock(node);
    if(firstBlock.isPresent()){
      firstBlock.get().accept(this);
    }
    return super.visit(node);
  }

  @Override public boolean visit(TypeDeclarationStatement node){
    final Optional<Block> firstBlock = findFirstBlock(node);
    if(firstBlock.isPresent()){
      firstBlock.get().accept(this);
    }
    return super.visit(node);
  }

  @Override public boolean visit(WhileStatement node){
    final Optional<Block> firstBlock = findFirstBlock(node);
    if(firstBlock.isPresent()){
      firstBlock.get().accept(this);
    }
    return super.visit(node);
  }


  private static Optional<Block> findFirstBlock(ASTNode node){

    if(node instanceof Block) return Optional.of(((Block)node));

    return Jdt.getChildren(node).stream()
      .filter(n -> (n instanceof Block))
      .map(n -> (Block) n).findFirst();
  }




  public List<Block> getCodeBlocks(){
    return blocks;
  }
}
