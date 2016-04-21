package com.vesperin.cue;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.builder.CliBuilder;
import com.github.rvesse.airline.parser.errors.ParseException;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
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
import com.vesperin.cue.cmds.CommandRunnable;
import com.vesperin.cue.cmds.ConceptsRecog;
import com.vesperin.cue.cmds.Typicality;
import com.vesperin.cue.text.WordCounter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.vesperin.cue.bsg.AstUtils.methodDeclaration;
import static com.vesperin.cue.utils.Similarity.similarityScore;

/**
 * @author Huascar Sanchez
 */
public class Cue {
  private static final double SMOOTHING_FACTOR = 0.3;
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
   * Determine the concepts (capped to 10 suggestions) that appear in a list of
   * sources.
   *
   * @param sources list of sources to inspect.
   * @return a new list of guessed concepts.
   */
  public List<String> assignedConcepts(List<Source> sources){
    return assignedConcepts(sources, 10);
  }

  /**
   * Determine the concepts that appear in a list of
   * sources.
   *
   * @param sources list of sources to inspect.
   * @return a new list of guessed concepts.
   */
  public List<String> assignedConcepts(List<Source> sources, int topK){
    final Set<String> addOns = Sets.newHashSet("main");

    return assignedConcepts(sources, topK, addOns);
  }


  /**
   * Determine the concepts that appear in a list of
   * sources.
   *
   * @param sources list of sources to inspect.
   * @param topK k most frequent concepts in the list of sources.
   * @return a new list of guessed concepts.
   */
  public List<String> assignedConcepts(List<Source> sources, int topK, Set<String> ignoredSet){
    final WordCounter counter = new WordCounter();

    for(Source each : sources){
      counter.addAll(assignedConcepts(each, ignoredSet));
    }

    return counter.mostFrequent(topK);

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

    return assignedConcepts(context, scope, 10, ImmutableSet.of());

  }

  /**
   * Determine the concepts (capped to 10 suggestions) that appear in a given source code.
   *
   * @param code source code to introspect.
   * @return a new list of guessed concepts.
   */
  public List<String> assignedConcepts(Source code, final Set<String> ignoredSet){
    final Context   context = parse(code);
    final Location  scope   = Locations.locate(code, context.getCompilationUnit());

    final Set<Location> blackList = context.locateMethods().stream()
      .filter(m ->
        ignoredSet.contains(
          methodDeclaration(m.getUnitNode()).getName().getIdentifier()
        )
      ).collect(Collectors.toSet());

    return assignedConcepts(context, scope, 10, blackList);

  }

  /**
   * Determine the concepts (capped to 10 suggestions) that appear in a given source code's region.
   *
   * @param scope code region of interest.
   * @return a new list of guessed concepts.
   */
  public List<String> assignedConcepts(Source code, Location scope){
    return assignedConcepts(code, scope, ImmutableSet.of());
  }

  /**
   * Determine the concepts (capped to 10 suggestions) that appear in a given source code's region.
   *
   * @param scope code region of interest.
   * @return a new list of guessed concepts.
   */
  public List<String> assignedConcepts(Source code, Location scope, Set<Location> blackList){
    return assignedConcepts(code, scope, 10, blackList);
  }

  /**
   * Determine the concepts that appear in a given context's region.
   *
   * @param scope code region of interest.
   * @param topK number of suggestions to retrieve.
   * @return a new list of guessed concepts.
   */
  public List<String> assignedConcepts(Source code, Location scope, int topK, Set<Location> blackList){
    final Context   context = parse(code);
    return assignedConcepts(context, scope, topK, blackList);
  }


  /**
   * Determine the concepts that appear in a given context's boundary.
   *
   * @param context parsed source code.
   * @param scope code region of interest.
   * @param topK number of suggestions to retrieve.
   * @return a new list of guessed concepts.
   */
  private List<String> assignedConcepts(Context context, Location scope, int topK, Set<Location> blackList){

    ensureValidInput(context, scope, topK);

    // focus on code region
    final Optional<UnitLocation> unitLocation = context.locateUnit(scope).stream().findFirst();
    if(!unitLocation.isPresent()) return ImmutableList.of();
    final UnitLocation locatedUnit = unitLocation.get();

    // generate interesting concepts
    return assignedConcepts(locatedUnit, blackList, topK);
  }

  private List<String> assignedConcepts(UnitLocation locatedUnit, Set<Location> blackAddons, int topK){
    // generate optimal segmentation graph
    final SegmentationGraph bsg = generateSegmentationGraph(locatedUnit);
    final Set<Location> blackList = bsg.blacklist(locatedUnit).stream()
      .collect(Collectors.toSet());

    if(!blackAddons.isEmpty()) {
      blackList.addAll(blackAddons);
    }

    return generateInterestingConcepts(topK, locatedUnit, blackList);
  }

  private List<String> generateInterestingConcepts(int topK, UnitLocation locatedUnit, Set<Location> blackList) {

    // collect frequent words outside the blacklist of locations
    final TokenIterator extractor   = new TokenIterator(blackList);
    locatedUnit.getUnitNode().accept(extractor);

    final WordCounter wordCounter = new WordCounter(extractor.getItems());

    return wordCounter.mostFrequent(topK);
  }

  private SegmentationGraph generateSegmentationGraph(UnitLocation locatedUnit) {
    final BlockSegmentationVisitor blockSegmentation = new BlockSegmentationVisitor(locatedUnit);
    locatedUnit.getUnitNode().accept(blockSegmentation);

    return blockSegmentation.getBSG();
  }

  private static void ensureValidInput(Context context, Location scope, int topK) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(scope);

    Preconditions.checkArgument(topK > 0);
  }

  /**
   * Finds the top k most typical implementation of some functionality in a set of
   * similar implementations of that functionality. It uses 0.3 as a default
   * bandwidth parameter.
   *
   * See {@link #typicalityQuery(List, double, int)} for additional details.
   *
   * @param similarCode a list of source code implementing similar functionality.
   * @param topK top k most typical implementations.
   * @return a new list of the most typical source code implementing a functionality.
   */
  public List<Source> typicalityQuery(List<Source> similarCode, int topK){
    return typicalityQuery(similarCode, SMOOTHING_FACTOR, topK);

  }

  /**
   * Finds the top k most typical implementation of some functionality in a set of
   * similar implementations of that functionality.
   *
   * Uses the idea of typicality analysis from psychology and cognition science
   * to source code ranking. This ranking is based on a simple typicality measure
   * introduced in:
   *
   * Ming Hua, Jian Pei, Ada W. C. Fu, Xuemin Lin, and Ho-Fung Leung. 2007.
   * Efficiently answering top-k typicality queries on large databases.
   * In Proceedings of the 33rd international conference on Very large
   * databases (VLDB '07). VLDB Endowment 890-901.
   *
   * @param similarCode a list of source code implementing similar functionality.
   * @param h bandwidth parameter (a.k.a., smoothing factor)
   * @param topK top k most typical implementations.
   * @return a new list of the most typical source code implementing a functionality.
   * @see {@code https://www.cs.sfu.ca/~jpei/publications/typicality-vldb07.pdf}
   */
  public List<Source> typicalityQuery(List<Source> similarCode, double h, int topK){

    if(similarCode.isEmpty()) return ImmutableList.of();
    if(topK <= 0)             return ImmutableList.of();

    final Map<Source, Double> T = new HashMap<>();

    for(Source code : similarCode){
      T.put(code, 0.0);
    }
    
    double t1  = 1.0d / (similarCode.size() - 1) * Math.sqrt(2.0 * Math.PI);
    double t2  = 2.0 * Math.pow(h, 2);

    int N = similarCode.size() - 1;
    int M = similarCode.size();

    assert N < M;

    for (int idx = 0; idx < N; idx++){
      for (Source oj : similarCode) {

        final Source oi = similarCode.get(idx);

        double w   = gaussianKernel(t1, t2, oi, oj);
        double Toi = T.get(oi) + w;
        double Toj = T.get(oj) + w;

        T.put(oi, Toi);
        T.put(oi, Toj);
      }
    }

    return T.keySet().stream()
      .sorted((a, b) -> Doubles.compare(T.get(b), T.get(a)))
      .limit(topK)
      .collect(Collectors.toList());
  }
  
  private static double gaussianKernel(double t1, double t2, Source oi, Source oj){
    return t1 * Math.exp(-(Math.pow(score(oi, oj), 2) / t2));
  }

  private static double score(Source a, Source b){
    return similarityScore(a.getContent(), b.getContent());
  }


  private static <T extends CommandRunnable> void execute(T cmd) {
    try {
      int exitCode = cmd.run();
      System.out.println();
      System.out.println("Exiting with Code " + exitCode);
      System.exit(exitCode);
    } catch (Throwable e) {
      System.err.println("Command threw error: " + e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  private static <T extends CommandRunnable> void executeCli(Cli<T> cli, String[] args) {
    try {
      T cmd = cli.parse(args);
      execute(cmd);
    } catch (ParseException e) {
      System.err.println("Parser error: " + e.getMessage());
    } catch (Throwable e) {
      System.err.println("Unexpected error: " + e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  public static void main(String[] args) {
    final CliBuilder<CommandRunnable> builder = Cli.<CommandRunnable>builder("cue")
      .withDescription("Cue CLI")
      .withCommand(Typicality.class)
      .withCommand(ConceptsRecog.class);

    executeCli(builder.build(), args);
  }

}
