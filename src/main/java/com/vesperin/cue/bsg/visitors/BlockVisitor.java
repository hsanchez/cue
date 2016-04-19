package com.vesperin.cue.bsg.visitors;

import com.vesperin.base.visitors.SkeletalVisitor;
import org.eclipse.jdt.core.dom.Block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    }
    return super.visit(node);
  }


  public List<Block> getCodeBlocks(){
    return blocks;
  }
}
