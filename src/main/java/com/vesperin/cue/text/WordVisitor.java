package com.vesperin.cue.text;

import com.google.common.collect.Sets;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.utils.Jdt;
import com.vesperin.base.visitors.SkeletalVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Huascar Sanchez
 */
public class WordVisitor extends SkeletalVisitor implements Iterable <Word> {

  private static final Set<StopWords> STOP = Sets.newHashSet(StopWords.ENGLISH, StopWords.JAVA);

  private final List<Word>      items;
  private final Set<String>     visited;
  private final Set<Location>   blackSet;

  /**
   * Constructs a token iterator/visitor.
   */
  public WordVisitor(){
    this(new HashSet<>());
  }

  /**
   * Constructs a token iterator/visitor.
   *
   * @param blackSet set of words to ignore.
   */
  public WordVisitor(Set<Location> blackSet){
    this.items            = new ArrayList<>();
    this.visited          = new HashSet<>();
    this.blackSet         = blackSet;
  }


  @Override public boolean visit(SimpleName simpleName) {

    final Location nodeLocation = Locations.locate(simpleName);
    final String identifier = simpleName.getIdentifier().replaceAll("(-?\\d+)|(\\+1)", "");

    if(visited.contains(identifier)) return false;
    final boolean underscored = identifier.split(Pattern.quote("_")).length == 1;
    final boolean onlyConsonants  = SpellChecker.onlyConsonants(identifier);
    final boolean tooSmall        = identifier.length() < 4;

    if((underscored && onlyConsonants) || tooSmall){
      visited.add(identifier);
      return false;
    }


    if(isValidLocation(simpleName) && !inBlackSet(nodeLocation)){

      if(!(identifier.endsWith("Exception") || identifier.equals("Throwable") || identifier.equals("Error"))){
        String[] split = identifier.split("((?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z]))|_");
        if(split.length == 1){
          split = split[0].split(Pattern.quote("_"));
        }

        for(String eachLabel : split){

          if(" ".equals(eachLabel) || eachLabel.isEmpty()
            || StopWords.isStopWord(STOP, eachLabel, eachLabel + "s"))
            continue;

          String currentLabel = eachLabel.toLowerCase(Locale.ENGLISH);

          if(SpellChecker.onlyConsonants(currentLabel) || !SpellChecker.containsWord(currentLabel)){
            final String newLabel = SpellChecker.suggestCorrection(currentLabel).toLowerCase();

            if(SpellChecker.similarity(currentLabel, newLabel) > 0.3f){
              currentLabel = newLabel;
            }
          }

          final String wordLiteral = currentLabel.toLowerCase(Locale.ENGLISH);
          final Word word = new Word(wordLiteral);
          updateWordWithMetadata(word, simpleName);
          getItems().add(word);
        }
      }

    }

    return false;
  }

  private static void updateWordWithMetadata(Word word, ASTNode node){
    final MethodDeclaration immediateParent = Jdt.parent(MethodDeclaration.class, node);
    final TypeDeclaration   bodyDeclaration = Jdt.parent(TypeDeclaration.class, node);

    if(!Objects.isNull(immediateParent) && !Objects.isNull(bodyDeclaration)){
      final String className = bodyDeclaration.getName().getIdentifier();
      final String methodName = immediateParent.getName().getIdentifier();
      final String source = className + "#" + methodName;
      word.add(source);
    }

  }

  private static boolean isValidLocation(ASTNode node){
    final int leftBound   = node.getStartPosition();
    final int rightBound  = leftBound + node.getLength();

    return leftBound >= 0 && rightBound >= leftBound;
  }


  public List<Word> getItems() {
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


  @Override public Iterator<Word> iterator() {
    return getItems().iterator();
  }
}
