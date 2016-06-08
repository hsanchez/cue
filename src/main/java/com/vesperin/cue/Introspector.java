package com.vesperin.cue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.vesperin.base.Context;
import com.vesperin.base.Source;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.locators.ProgramUnitLocation;
import com.vesperin.base.locators.UnitLocation;
import com.vesperin.cue.segment.BlockSegmentationVisitor;
import com.vesperin.cue.segment.SegmentationGraph;
import com.vesperin.cue.spi.SourceSelection;
import com.vesperin.cue.text.TokenIterator;
import com.vesperin.cue.text.WordCounter;
import com.vesperin.cue.utils.Similarity;
import com.vesperin.cue.utils.Sources;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.vesperin.cue.utils.AstUtils.methodName;
import static com.vesperin.cue.utils.Similarity.similarityScore;

/**
 * @author Huascar Sanchez
 */
public interface Introspector {

  /**
   * Determine the concepts (capped to 10 suggestions) that appear in a list of
   * sources.
   *
   * @param sources list of sources to inspect.
   * @return a new list of guessed concepts.
   */
  default List<String> assignedConcepts(List<Source> sources){
    return assignedConcepts(10, sources);
  }

  /**
   * Determine the concepts that appear in a list of
   * sources.
   *
   * @param topK k most frequent concepts in the list of sources.
   * @param sources list of sources to inspect.
   * @return a new list of guessed concepts.
   */
  default List<String> assignedConcepts(int topK, List<Source> sources){
    return assignedConcepts(topK, sources, ImmutableSet.of());
  }


  /**
   * Determine the concepts that appear in oi list of
   * sources.
   *
   * @param topK k most frequent concepts in the list of sources.
   * @param sources list of sources to inspect.
   * @param relevantSet set of relevant method names
   * @return oi new list of guessed concepts.
   */
  default List<String> assignedConcepts(int topK, List<Source> sources, Set<String> relevantSet){

    final ExecutorService service = newExecutorService(sources.size());
    final WordCounter     counter = new WordCounter();

    for(Source each : sources){
      service.execute(() -> counter.addAll(assignedConcepts(each, relevantSet)));
    }

    // shuts down the executor service
    shutdownExecutorService(service);

    return counter.mostFrequent(topK);

  }

  static ExecutorService newExecutorService(int scale){
    final int cpus       = Runtime.getRuntime().availableProcessors();
    scale                = scale > 10 ? 10 : scale;
    final int maxThreads = ((cpus * scale) > 0 ? (cpus * scale) : 1);

    return Executors.newFixedThreadPool(maxThreads);
  }

  static void shutdownExecutorService(ExecutorService service){
    // wait for all of the executor threads to finish
    service.shutdown();

    try {
      if (!service.awaitTermination(60, TimeUnit.SECONDS)) {
        // pool didn't terminate after the first try
        service.shutdownNow();
      }


      if (!service.awaitTermination(60, TimeUnit.SECONDS)) {
        // pool didn't terminate after the second try
        System.out.println("ERROR: executor service did not terminate after a second try.");
      }
    } catch (InterruptedException ex) {
      service.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }


  /**
   * Determine the concepts (capped to 10 suggestions) that appear in a given source code.
   *
   * @param code source code to introspect.
   * @return a new list of guessed concepts.
   */
  default List<String> assignedConcepts(Source code){
    return assignedConcepts(code, ImmutableSet.of());
  }


  /**
   * Determine the concepts (capped to 10 suggestions) that appear in a given source code.
   *
   * @param code source code to introspect.
   * @param relevant set of relevant method names. At least one match should exist.
   * @return a new list of guessed concepts.
   */
  default List<String> assignedConcepts(Source code, final Set<String> relevant){
    final Context   context = Sources.from(code);

    final UnitLocation unit = relevant.isEmpty()
      ? locatedUnit(context)
      : locatedMethod(context, relevant);

    if(unit == null) return ImmutableList.of();

    return assignedConcepts(unit, 10);
  }

  /**
   * Determine the top k most concepts that appear within a located unit.
   *
   * @param locatedUnit list of sources to inspect.
   * @param topK k most frequent concepts in the list of sources.
   * @return a new list of guessed concepts.
   */
  default List<String> assignedConcepts(Location locatedUnit, int topK){
    final UnitLocation  unitLocation  = (UnitLocation) Objects.requireNonNull(locatedUnit);
    final Set<Location> irrelevantSet = generateIrrelevantSet(unitLocation);

    return interestingConcepts(topK, unitLocation, irrelevantSet);
  }


  /**
   * Generates the set of irrelevant locations this introspector is not
   * interested in exploring.
   *
   * @param unitLocation the located unit of interest.
   * @return a new set of irrelevant locations.
   */
  static Set<Location> generateIrrelevantSet(UnitLocation unitLocation){
    Objects.requireNonNull(unitLocation);
    final SegmentationGraph bsg = generateSegmentationGraph(unitLocation);
    return bsg.irrelevantSet(unitLocation);
  }

  /**
   * Generates a new segmentation graph based on a located program unit..
   *
   * @param unitLocation located unit.
   * @return a new {@link SegmentationGraph segmentation} graph.
   */
  static SegmentationGraph generateSegmentationGraph(UnitLocation unitLocation){
    final BlockSegmentationVisitor visitor = new BlockSegmentationVisitor(unitLocation);

    unitLocation.getUnitNode().accept(visitor);

    return visitor.getBlockSegmentationGraph();
  }

  /**
   * Determine the top k most interesting concepts that appear within a located unit.
   *
   * @param topK k most frequent concepts in the list of sources.
   * @param located located unit.
   * @param irrelevantSet set of irrelevant names
   * @return a new list of interesting concepts.
   */
  default List<String> interestingConcepts(int topK, UnitLocation located,
          Set<Location> irrelevantSet){
    // collect frequent words outside the blacklist of locations
    final TokenIterator extractor   = new TokenIterator(irrelevantSet);
    located.getUnitNode().accept(extractor);

    final WordCounter wordCounter = new WordCounter(extractor.getItems());

    return wordCounter.mostFrequent(topK);
  }

  /**
   * Finds those typical source objects in T (most representative) that are different from each
   * other but jointly represent the whole set of similar objects S;  The produced set R is a
   * small subset of T.
   *
   * @param resultSet the set of source objects implementing a similar functionality.
   * @param domain relevant method names.
   * @return a smaller list of typical source objects representing the whole set of source objects
   *  implementing a similar functionality.
   */
  default List<Source> issueRepresentativeQuery(Set<Source> resultSet, Set<String> domain){
    return issueRepresentativeQuery(5/*optimization: 5 most typical are enough*/, resultSet, domain);
  }

  /**
   * Finds those typical source objects in T (most representative) that are different from each
   * other but jointly represent the whole set of similar objects S;  The produced set R is a
   * small subset of T.
   *
   * @param topk k most typical source object in the result set.
   * @param resultSet the set of source objects implementing a similar functionality.
   * @param domain relevant method names.
   * @return a smaller list of typical source objects representing the whole set of source objects
   *  implementing a similar functionality.
   */
  default List<Source> issueRepresentativeQuery(int topk, Set<Source> resultSet, Set<String> domain) {
    final Map<Source, List<Source>> region = interestingRegion(topk, resultSet, domain);

    final Comparator<Map.Entry<Source, List<Source>>> byValue =
      (entry1, entry2) ->
        Ints.compare(entry1.getValue().size(), entry2.getValue().size());

    return region.entrySet().stream()
      .sorted(byValue.reversed())
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());
  }

  /**
   * Given two sets, R (resultSet) and T (typicalSet), find the representing region for
   * each element o in T. This region of o corresponds to its closest object e in {R - T}.
   *
   * @param topk k most typical source object in the result set.
   * @param resultSet the set of source objects implementing a similar functionality.
   * @param domain relevant method names.
   * @return the representing region for members in T.
   */
  default Map<Source, List<Source>> interestingRegion(int topk, Set<Source> resultSet,
          Set<String> domain) {

    final List<Source>  topKList    = issueTypicalityQuery(topk/*tunable*/, resultSet, domain);

    final Set<Source>   typicalSet  = new LinkedHashSet<>();
    typicalSet.addAll(topKList);

    assert topKList.size() == typicalSet.size();

    return interestingRegion(resultSet, typicalSet, domain);
  }

  /**
   * Given two sets, R (resultSet) and T (typicalSet), and domain of relevant method names, find
   * the representing region for each element o in T. This region of o corresponds to its
   * closest (or most similar) object e in {R - T}.
   *
   * @param resultSet a set of source objects implementing a similar functionality.
   * @param relevant relevant methods names to introspect
   * @return the representing region for members in T.
   */
  default Map<Source, List<Source>> interestingRegion(Set<Source> resultSet, Set<Source> typicalitySet,
          Set<String> relevant) {

    final Set<Source> difference = Sets.difference(resultSet, typicalitySet);

    final Map<Source, String> segments = new HashMap<>();
    for(Source each : resultSet){
      segments.put(each, relevantSegments(each, relevant));
    }

    final Map<Source, List<Source>> region = new HashMap<>();

    for(Source e : difference){
      Source max = null;
      for(Source o : typicalitySet){
        if(max == null){
          max = o;
        } else {
          if(score(e, o, segments) > score(max, o, segments)){
            max = o;
          }
        }
      }

      if(!region.containsKey(max)){
        region.put(max, Lists.newArrayList(e));
      } else {
        region.get(max).add(e);
      }
    }

    return region;
  }

  /**
   * Finds the top k most typical implementation of some functionality in a set of
   * similar implementations of that functionality. It uses 0.3 as a default
   * bandwidth parameter.
   *
   * See {@link #issueTypicalityQuery(int, Set, Processor)} for additional details.
   *
   * @param topK top k most typical implementations.
   * @param resultSet a set of source objects implementing a similar functionality.
   * @param relevant relevant methods names to introspect
   * @return a new list of k most typical source objects implementing a similar functionality.
   */
  default List<Source> issueTypicalityQuery(int topK, Set<Source> resultSet, Set<String> relevant){
    return issueTypicalityQuery(topK, 0.3, resultSet, relevant);
  }

  /**
   * Finds the top k most typical implementation of some functionality in a set of
   * similar implementations of that functionality. It uses 0.3 as a default
   * bandwidth parameter.
   *
   * @param topK top k most typical implementations.
   * @param h smoothing factor
   * @param resultSet a set of source objects implementing a similar functionality.
   * @param relevant relevant methods names to introspect
   * @return a new list of k most typical source objects implementing a similar functionality.
   */
  default List<Source> issueTypicalityQuery(int topK, double h, Set<Source> resultSet, Set<String> relevant){
    return issueTypicalityQuery(topK, resultSet, new SegmentsTypicalityProcessor(h, relevant));
  }



  /**
   * Finds the top k most typical implementation of some functionality in a set of
   * similar implementations of that functionality.
   *
   * Uses the idea of typicality analysis from psychology and cognition science, and
   * query processing to source code ranking.
   *
   * See:
   * Ming Hua, Jian Pei, Ada W. C. Fu, Xuemin Lin, and Ho-Fung Leung. 2007.
   * Efficiently answering top-k typicality queries on large databases.
   * In Proceedings of the 33rd international conference on Very large
   * databases (VLDB '07). VLDB Endowment 890-901.
   *
   * @param resultSet a set of source code implementing similar functionality.
   * @param topK top k most typical implementations.
   * @param queryProcessor typicality query's processor.
   * @return a new list of the most typical source code implementing a functionality.
   * @see {@code https://www.cs.sfu.ca/~jpei/publications/typicality-vldb07.pdf}
   */
  default <T> List<Source> issueTypicalityQuery(int topK, Set<Source> resultSet,
                                                Processor <T> queryProcessor){
    return queryProcessor.process(topK, resultSet);
  }

  static UnitLocation locatedMethod(Context context, final Set<String> relevant){
    return context.locateMethods().stream()
      .filter(m -> relevant.contains(methodName(m.getUnitNode())))
      .findFirst()
      .orElse(null);
  }

  static UnitLocation locatedUnit(Context context){
    return new ProgramUnitLocation(
      context.getCompilationUnit(),
      Locations.locate(context.getCompilationUnit())
    );
  }

  /**
   /**
   * Pulls the code of some method of interest.
   *
   * @param code source file
   * @param relevant relevant method names
   * @return the method snippet.
   */
  static String relevantSegments(Source code, Set<String> relevant){
    final Context context = Sources.from(code);

    final UnitLocation unit   = (relevant.isEmpty()
      ? locatedUnit(context)
      : locatedMethod(context, relevant)
    );

    if(Objects.isNull(unit)) // returns nothing
      return "";

    final SegmentationGraph graph     = generateSegmentationGraph(unit);
    final List<Location>    whiteList = graph.relevantSet(unit).stream()
      .collect(Collectors.toList());

    final SourceSelection selection = new SourceSelection(whiteList);

    return selection.toCode();
  }

  static float score(Source a, Source b, Map<Source, String> summaries){
    return Similarity.similarityScore(
      summaries.get(a), summaries.get(b)
    );
  }

  /**
   * A type of processor for typicality queries.
   * @param <T> feature type
   */
  interface Processor <T> {

    /**
     * Generates a feature for a source and its relevant method.
     *
     * @param source source to be processed.
     * @return a new feature.
     */
    T from(Source source);

    /**
     * Pre-computes a list of features to be fed into the processor.
     *
     * @param sources a set of source objects implementing a similar functionality.
     * @return a new list of features for a set of source objects and its relevant method names.
     */
    default Set<T> from(Set<Source> sources){
      return sources.stream()
        .map(this::from)
        .collect(Collectors.toSet());
    }

    /**
     * Process a typicality query. A typicality query finds the top k most typical
     * implementation of some functionality in a set of implementations with similar
     * functionality.
     *
     * @param topK k most typical/representative source objects, where k cannot
     *             be greater than the size of the sources set.
     * @param sources a set of source objects implementing a similar functionality.
     * @return a list of relevant source objects, ranked by some given score. Different
     *    scores will be implemented by the implementors of this type.
     * @throws IllegalArgumentException if topK > Size(sources)
     */
    List<Source> process(int topK, Set<Source> sources);
  }


  /**
   * The feature type. A feature is a characteristic relevant to the typicality
   * analysis process.
   *
   * @param <T> parameter type representing the actual type of data held by this feature.
   */
  interface Feature <T> {
    /**
     * @return original source of data.
     */
    Source source();

    /**
     * @return feature's data.
     */
    T data();
  }

  /**
   * Default feature based on source code's content.
   */
  class CodeFeature implements Feature <String> {
    private final Source source;
    private final String data;

    CodeFeature(Source source, String data){
      this.source = source;
      this.data   = data;
    }

    @Override public Source source() {
      return source;
    }

    @Override public String data() {
      return data;
    }
  }


  /**
   * Default implementation of typicality analysis.
   */
  class SegmentsTypicalityProcessor implements Processor <Feature<String>> {
    private final double      h;
    private final Set<String> relevant;


    /**
     * Construct a new Content-based Typicality Processor
     *
     * @param h smoothing factor
     * @param relevant relevant method names
     */
    SegmentsTypicalityProcessor(double h, Set<String> relevant){
      this.h        = h;
      this.relevant = relevant;
    }

    @Override public Feature<String> from(Source source) {
      return new CodeFeature(source, relevantSegments(source, relevant));
    }


    @Override public List<Source> process(int topK, Set<Source> sources) {

      if(sources.isEmpty()) return ImmutableList.of();
      if(topK <= 0)         return ImmutableList.of();

      final Map<Feature<String>, Double> T = new HashMap<>();

      final Set<Feature<String>> features = from(sources);

      // Compute the cartesian product of the sources object
      final Set<List<Feature<String>>> cartesian = Sets.cartesianProduct(
        Arrays.asList(features, features)
      );


      // Initialize these features' scores
      for(Feature<String> code : features){
        T.put(code, 0.0);
      }

      assert features.size() == sources.size();

      double t1  = 1.0d / (features.size() - 1) * Math.sqrt(2.0 * Math.PI);
      double t2  = 2.0 * Math.pow(h, 2);

      for(List<Feature<String>> each : cartesian){
        final Feature<String> oi = each.get(0);
        final Feature<String> oj = each.get(1);

        final Pair<String> p = new Pair<>(oi, oj);

        double w = gaussianKernel(t1, t2, p);
        double Toi = T.get(oi) + w;
        double Toj = T.get(oj) + w;

        T.put(oi, Toi);
        T.put(oj, Toj);
      }

      final List<Feature<String>> result = T.keySet().stream()
        .sorted((a, b) -> Double.compare(T.get(b), T.get(a)))
        .limit(topK)
        .collect(Collectors.toList());

      return result.stream()
        .map(Feature::source)
        .collect(Collectors.toList());
    }


    private static double gaussianKernel(double t1, double t2, Pair<String> pair){
      return t1 * Math.exp(-(Math.pow(score(pair.oi, pair.oj), 2) / t2));
    }

    private static double score(Feature<String> a, Feature<String> b){
      return similarityScore(a.data(), b.data());
    }

    @Override public String toString() {
      return "SegmentsTypicalityProcessor (smoothingFactor = " + h + ")";
    }
  }

  /**
   * Record object that tracks features.
   *
   * @param <T> object type
   */
  class Pair <T> {
    Feature<T> oi = null;
    Feature<T> oj = null;

    Pair(Feature<T> oi, Feature<T> oj){
      this.oi = oi;
      this.oj = oj;
    }

    @Override public int hashCode() {
      return Objects.hashCode(oi.data()) * Objects.hashCode(oj.data());
    }

    @Override public boolean equals(Object obj) {
      if(!(obj instanceof Pair)) return false;

      @SuppressWarnings("unchecked")
      final Pair<T> other = (Pair<T>) obj; // unchecked warning

      final boolean sameAA = Objects.equals(oi.data(), other.oi.data());
      final boolean sameAB = Objects.equals(oi.data(), other.oj.data());
      final boolean sameBA = Objects.equals(oj.data(), other.oi.data());
      final boolean sameBB = Objects.equals(oj.data(), other.oj.data());

      return (sameAA ||  sameAB || sameBA || sameBB);
    }
  }
}
