package com.vesperin.cue;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.vesperin.base.Context;
import com.vesperin.base.EclipseJavaParser;
import com.vesperin.base.JavaParser;
import com.vesperin.base.Source;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.locators.UnitLocation;
import com.vesperin.cue.bsg.SegmentationGraph;
import com.vesperin.cue.bsg.visitors.BlockSegmentationVisitor;
import com.vesperin.cue.bsg.visitors.TokenIterator;
import com.vesperin.cue.text.WordCounter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class Cue {
  private final JavaParser parser;

  public Cue(){
    this(new EclipseJavaParser());
  }

  private Cue(JavaParser parser){
    this.parser = parser;
  }

  Context parse(Source code){
    return parser.parseJava(code);
  }

  /**
   * Determine the concepts (capped to 10 suggestions) that appear in a given source code.
   *
   * @param code source code to introspect.
   * @return a new list of guessed concepts.
   */
  public List<String> assignedConcepts(Source code){
    final Context   context = parse(code);
    final Location  scope   = Locations.locate(code, context.getCompilationUnit());

    return assignedConcepts(context, scope, 10);

  }

  /**
   * Determine the concepts (capped to 10 suggestions) that appear in a given source code's region.
   *
   * @param scope code region of interest.
   * @return a new list of guessed concepts.
   */
  public List<String> assignedConcepts(Source code, Location scope){
    return assignedConcepts(code, scope, 10);
  }

  /**
   * Determine the concepts that appear in a given context's region.
   *
   * @param scope code region of interest.
   * @param topK number of suggestions to retrieve.
   * @return a new list of guessed concepts.
   */
  public List<String> assignedConcepts(Source code, Location scope, int topK){
    final Context   context = parse(code);
    return assignedConcepts(context, scope, topK);
  }


  /**
   * Determine the concepts that appear in a given context's boundary.
   *
   * @param context parsed source code.
   * @param scope code region of interest.
   * @param topK number of suggestions to retrieve.
   * @return a new list of guessed concepts.
   */
  private List<String> assignedConcepts(Context context, Location scope, int topK){

    Objects.requireNonNull(context);
    Objects.requireNonNull(scope);

    Preconditions.checkArgument(topK > 0);

    final Optional<UnitLocation> unitLocation = context.locateUnit(scope).stream().findFirst();
    if(!unitLocation.isPresent()) return ImmutableList.of();

    final UnitLocation locatedUnit = unitLocation.get();

    // catch segments in code
    final BlockSegmentationVisitor blockSegmentation = new BlockSegmentationVisitor(locatedUnit);
    locatedUnit.getUnitNode().accept(blockSegmentation);

    final SegmentationGraph bsg = blockSegmentation.getBSG();

    // generate non interesting locations
    final Set<Location> blackList = bsg.blacklist(locatedUnit).stream()
      .collect(Collectors.toSet());

    // collect frequent words outside the blacklist of locations
    final TokenIterator extractor   = new TokenIterator(blackList);
    locatedUnit.getUnitNode().accept(extractor);

    final WordCounter wordCounter = new WordCounter(extractor.getItems());


    return wordCounter.mostFrequent(topK);
  }

}
