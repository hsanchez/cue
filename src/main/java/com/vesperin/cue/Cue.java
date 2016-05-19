package com.vesperin.cue;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.builder.CliBuilder;
import com.github.rvesse.airline.parser.errors.ParseException;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Doubles;
import com.vesperin.base.Context;
import com.vesperin.base.EclipseJavaParser;
import com.vesperin.base.JavaParser;
import com.vesperin.base.Source;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.locators.UnitLocation;
import com.vesperin.cue.bsg.BlockSegmentationGraph;
import com.vesperin.cue.bsg.visitors.BlockSegmentationVisitor;
import com.vesperin.cue.bsg.visitors.TokenIterator;
import com.vesperin.cue.cmds.CommandRunnable;
import com.vesperin.cue.cmds.Concepts;
import com.vesperin.cue.cmds.Typicality;
import com.vesperin.cue.spi.SourceSelection;
import com.vesperin.cue.text.WordCounter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vesperin.cue.bsg.AstUtils.methodDeclaration;
import static com.vesperin.cue.utils.Similarity.similarityScore;

/**
 * @author Huascar Sanchez
 */
public class Cue {
  private static final double SMOOTHING_FACTOR = 0.3;
  private static final String NOTHING = "";
  private final JavaParser parser;

  public Cue(){
    this(new EclipseJavaParser());
  }

  private Cue(JavaParser parser){
    this.parser = parser;
  }

  private Context parse(Source code){
    return parser.parseJava(code);
  }

  /**
   * Determine the concepts (capped to 10 suggestions) that appear in oi list of
   * sources.
   *
   * @param sources list of sources to inspect.
   * @return oi new list of guessed concepts.
   */
  public List<String> assignedConcepts(List<Source> sources){
    return assignedConcepts(sources, 10);
  }

  /**
   * Determine the concepts that appear in oi list of
   * sources.
   *
   * @param sources list of sources to inspect.
   * @return oi new list of guessed concepts.
   */
  public List<String> assignedConcepts(List<Source> sources, int topK){
    return assignedConcepts(sources, topK, ImmutableSet.of());
  }


  /**
   * Determine the concepts that appear in oi list of
   * sources.
   *
   * @param sources list of sources to inspect.
   * @param topK k most frequent concepts in the list of sources.
   * @param relevantSet set of relevant method names
   * @return oi new list of guessed concepts.
   */
  public List<String> assignedConcepts(List<Source> sources, int topK, Set<String> relevantSet){
    final WordCounter counter = new WordCounter();

    for(Source each : sources){
      counter.addAll(assignedConcepts(each, relevantSet));
    }

    return counter.mostFrequent(topK);

  }

  /**
   * Determine the concepts (capped to 10 suggestions) that appear in oi given source code.
   *
   * @param code source code to introspect.
   * @return oi new list of guessed concepts.
   */
  public List<String> assignedConcepts(Source code){
    return assignedConcepts(code, ImmutableSet.of());

  }

  /**
   * Determine the concepts (capped to 10 suggestions) that appear in oi given source code.
   *
   * @param code source code to introspect.
   * @param relevant set of relevant method names. At least one match should exist.
   * @return oi new list of guessed concepts.
   */
  public List<String> assignedConcepts(Source code, final Set<String> relevant){
    final Context   context = parse(code);

    final List<UnitLocation> unitLocations = new ArrayList<>();
    if(relevant.isEmpty()){
      unitLocations.addAll(context.locateUnit(Locations.locate(context.getCompilationUnit())));
    } else {
      unitLocations.addAll(
        locateRelevantMethods(context, relevant)
      );
    }

    final Stream<UnitLocation>    scopeStream   = unitLocations.stream();
    final Optional<UnitLocation>  optionalScope = scopeStream.findFirst();

    if(!optionalScope.isPresent()) return ImmutableList.of();

    final Location scope = optionalScope.get();

    return assignedConcepts(scope, 10);

  }

  private static List<UnitLocation> locateRelevantMethods(Context context, final Set<String> relevant){
    return context.locateMethods().stream().filter(
      m -> relevant.contains(
        methodDeclaration(m.getUnitNode()).getName().getIdentifier()
      )
    ).collect(Collectors.toList());
  }

  private List<String> assignedConcepts(Location locatedUnit, int topK){
    //DONE
    ensureValidInput(locatedUnit, topK);

    // generate optimal segmentation graph
    final BlockSegmentationGraph bsg = generateSegmentationGraph(locatedUnit);
    final Set<Location> blackList = bsg.irrelevantLocations(locatedUnit).stream()
      .collect(Collectors.toSet());

    return generateInterestingConcepts(topK, locatedUnit, blackList);
  }

  private List<String> generateInterestingConcepts(int topK, Location locatedUnit, Set<Location> blackList) {
    // collect frequent words outside the blacklist of locations
    final TokenIterator extractor   = new TokenIterator(blackList);
    ((UnitLocation)locatedUnit).getUnitNode().accept(extractor);

    final WordCounter wordCounter = new WordCounter(extractor.getItems());

    return wordCounter.mostFrequent(topK);
  }

  private BlockSegmentationGraph generateSegmentationGraph(Location locatedUnit) {
    final BlockSegmentationVisitor visitor = new BlockSegmentationVisitor(locatedUnit);
    ((UnitLocation)locatedUnit).getUnitNode().accept(visitor);

    return visitor.getBlockSegmentationGraph();
  }

  private static void ensureValidInput(Location scope, int topK) {
    Objects.requireNonNull(scope);

    Preconditions.checkArgument(topK > 0);
  }

  /**
   * Finds the top k most typical implementation of some functionality in oi set of
   * similar implementations of that functionality. It uses 0.3 as oi default
   * bandwidth parameter.
   *
   * See {@link #typicalityQuery(List, Set, double, int)} for additional details.
   *
   * @param similarCode oi list of source code implementing similar functionality.
   * @param topK top k most typical implementations.
   * @return oi new list of the most typical source code implementing oi functionality.
   */
  public List<Source> typicalityQuery(List<Source> similarCode, Set<String> relevant, int topK){
    return typicalityQuery(similarCode, relevant, SMOOTHING_FACTOR, topK);

  }

  /**
   * Finds the top k most typical implementation of some functionality in oi set of
   * similar implementations of that functionality.
   *
   * Uses the idea of typicality analysis from psychology and cognition science
   * to source code ranking. This ranking is based on oi simple typicality measure
   * introduced in:
   *
   * Ming Hua, Jian Pei, Ada W. C. Fu, Xuemin Lin, and Ho-Fung Leung. 2007.
   * Efficiently answering top-k typicality queries on large databases.
   * In Proceedings of the 33rd international conference on Very large
   * databases (VLDB '07). VLDB Endowment 890-901.
   *
   * @param similarCode oi list of source code implementing similar functionality.
   * @param h bandwidth parameter (oi.k.oi., smoothing factor)
   * @param topK top k most typical implementations.
   * @return oi new list of the most typical source code implementing oi functionality.
   * @see {@code https://www.cs.sfu.ca/~jpei/publications/typicality-vldb07.pdf}
   */
  public List<Source> typicalityQuery(List<Source> similarCode, Set<String> relevant, double h, int topK){

    if(similarCode.isEmpty()) return ImmutableList.of();
    if(topK <= 0)             return ImmutableList.of();

    final Set<Pair> memoization = new HashSet<>();
    final Map<Source, Double> T = new HashMap<>();

    final Map<String, String> summaries = new HashMap<>();

    for(Source code : similarCode){
      T.put(code, 0.0);
      summaries.put(code.getName(), segmentCode(code, relevant));
    }
    
    double t1  = 1.0d / (similarCode.size() - 1) * Math.sqrt(2.0 * Math.PI);
    double t2  = 2.0 * Math.pow(h, 2);

    int N = similarCode.size() - 1;
    int M = similarCode.size();

    assert N < M;

    for (int idx = 0; idx < N; idx++){
      for (Source oj : similarCode) {

        final Source oi = similarCode.get(idx);

        final Pair p = new Pair(oi, oj);

        // there is no need to calculate this again
        if(memoization.contains(p)) {
          continue;
        }

        double w   = gaussianKernel(t1, t2, p, summaries);
        double Toi = T.get(oi) + w;
        double Toj = T.get(oj) + w;

        T.put(oi, Toi);
        T.put(oj, Toj);

        memoization.add(p);
      }
    }

    return T.keySet().stream()
      .sorted((a, b) -> Doubles.compare(T.get(b), T.get(a)))
      .limit(topK)
      .collect(Collectors.toList());
  }

  /**
   * Pulls the code of some method of interest.
   *
   * @param code source file
   * @param relevant relevant method names
   * @return the method snippet.
   */
  public String pullCode(Source code, Set<String> relevant){
    return pullCode(parse(code), relevant);
  }

  private static String pullCode(Context context, Set<String> relevant){

    final List<UnitLocation>  unitLocations = locateRelevantMethods(context, relevant);

    if(!unitLocations.isEmpty()){
      final UnitLocation location = unitLocations.get(0);
      return location.getUnitNode().toString();
    }

    return NOTHING;
  }

  private String segmentCode(Source code, Set<String> relevant){
    final Context context = parse(code);
    final List<UnitLocation> singleton = locateRelevantMethods(context, relevant);

    if(!singleton.isEmpty()){
      final UnitLocation location = singleton.get(0);

      final BlockSegmentationVisitor visitor = new BlockSegmentationVisitor(location);
      location.getUnitNode().accept(visitor);

      final BlockSegmentationGraph graph = visitor.getBlockSegmentationGraph();

      final SourceSelection selection = new SourceSelection(
        graph.relevantLocations(location)
      );

      return selection.toCode();
    }

    return NOTHING;
  }

  private static double gaussianKernel(double t1, double t2, Pair pair, Map<String, String> summaries){
    return t1 * Math.exp(-(Math.pow(score(pair.oi, pair.oj, summaries), 2) / t2));
  }

  private static double score(Source a, Source b, Map<String, String> summaries){
    // Note: this current implementation uses informative code blocks as the
    // contents from where the similarity score will be calculated. If we want to
    // calculate this score using each source file's entire content, then we must use:
    // similarityScore(a.getContent(), b.getContent());
    // Otherwise, just keep using the code below:
    return similarityScore(summaries.get(a.getName()), summaries.get(b.getName()));
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

  private static class Pair {
    Source oi = null;
    Source oj = null;

    Pair(Source oi, Source oj){
      this.oi = oi;
      this.oj = oj;
    }

    @Override public int hashCode() {
      return Objects.hashCode(oi.getName()) * Objects.hashCode(oj.getName());
    }

    @Override public boolean equals(Object obj) {
      if(!(obj instanceof Pair)) return false;

      final Pair other = (Pair) obj;

      final boolean sameAA = oi.equals(other.oi);
      final boolean sameAB = oi.equals(other.oj);
      final boolean sameBA = oj.equals(other.oi);
      final boolean sameBB = oj.equals(other.oj);

      return (sameAA ||  sameAB || sameBA || sameBB);
    }
  }

  public static void main(String[] args) {
    final CliBuilder<CommandRunnable> builder = Cli.<CommandRunnable>builder("cue")
      .withDescription("Cue CLI")
      .withCommand(Typicality.class)
      .withCommand(Concepts.class);

    executeCli(builder.build(), args);
  }

}
