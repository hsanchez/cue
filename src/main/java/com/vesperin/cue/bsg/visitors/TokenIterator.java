package com.vesperin.cue.bsg.visitors;

import com.google.common.collect.Sets;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.visitors.SkeletalVisitor;
import com.vesperin.cue.text.SpellChecker;
import com.vesperin.cue.text.StopWords;
import org.eclipse.jdt.core.dom.SimpleName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class TokenIterator extends SkeletalVisitor implements Iterable <String> {
  private final List<String> items = new ArrayList<>();
  private final Set<SimpleName> visited = new HashSet<>();

  private final Set<Location> blackSet;

  public TokenIterator(){
    this(new HashSet<>());
  }

  public TokenIterator(Set<Location> blackSet){
    this.blackSet = blackSet;
  }


  @Override public boolean visit(SimpleName simpleName) {
    final Location nodeLocation = Locations.locate(simpleName);
    if(!visited.contains(simpleName) &&
      !inBlackSet(nodeLocation) && simpleName.getIdentifier().length() > 2){

      final String identifier = simpleName.getIdentifier();

      if(!(identifier.endsWith("Exception") || identifier.equals("Throwable") || identifier.equals("Error"))){
        final String[] split = simpleName.getIdentifier().split("((?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z]))|_");

        for(String eachLabel : split){

          if(" ".equals(eachLabel) || eachLabel.isEmpty() || StopWords.isStopWord(Sets.newHashSet(), eachLabel)) continue;

          String currentLabel = eachLabel;
          if(SpellChecker.onlyConsonants(currentLabel) || !SpellChecker.containsWord(currentLabel)){
            final String newLabel = SpellChecker.suggestCorrection(currentLabel.toLowerCase()).toLowerCase();

            if(SpellChecker.similarity(currentLabel, newLabel) > 0.3f){
              currentLabel = newLabel;
            }


          }

          getItems().add(currentLabel.toLowerCase());
        }

        visited.add(simpleName);

      }

    }


    return super.visit(simpleName);
  }


  public List<String> getItems() {
    return items;
  }



  private boolean inBlackSet(Location nodeLocation){
    for(Location each : blackSet){
      if(each.same(nodeLocation) || Locations.covers(each, nodeLocation)){
        return true;
      }
    }

    return false;
  }


  @Override public Iterator<String> iterator() {
    return getItems().iterator();
  }
}
