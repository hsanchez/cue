package com.vesperin.cue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.vesperin.base.Context;
import com.vesperin.base.Source;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.locators.ProgramUnitLocation;
import com.vesperin.base.locators.UnitLocation;
import com.vesperin.cue.segment.Segments;
import com.vesperin.cue.spi.SourceSelection;
import com.vesperin.cue.utils.Sources;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.vesperin.cue.utils.AstUtils.methodName;
import static com.vesperin.text.utils.Similarity.similarityScore;
import static java.util.stream.Collectors.toList;

/**
 * @author Huascar Sanchez
 */
public interface Introspector {

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
  default List<Source> representativeSources(Set<Source> resultSet, Set<String> domain){
    return representativeSources(5/*optimization: 5 most typical are enough*/, resultSet, domain);
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
  default List<Source> representativeSources(int topk, Set<Source> resultSet, Set<String> domain) {
    final Map<Source, List<Source>> region = interestingRegion(topk, resultSet, domain);

    final Comparator<Map.Entry<Source, List<Source>>> byValue =
      (entry1, entry2) ->
        Ints.compare(entry1.getValue().size(), entry2.getValue().size());

    return region.entrySet().stream()
      .sorted(byValue.reversed())
      .map(Map.Entry::getKey)
      .collect(toList());
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

    final List<Source>  topKList    = typicalityQuery(topk/*tunable*/, resultSet, domain);

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
      segments.put(each, segmentsCode(each, relevant));
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
   * See {@link #typicalityQuery(Set, TypicalityProcessor)} for additional details.
   *
   * @param topK top k most typical implementations.
   * @param resultSet a set of source objects implementing a similar functionality.
   * @param relevant relevant methods names to introspect
   * @return a new list of k most typical source objects implementing a similar functionality.
   */
  default List<Source> typicalityQuery(int topK, Set<Source> resultSet, Set<String> relevant){
    return typicalityQuery(topK, 0.3, resultSet, relevant);
  }

  /**
   * Finds the top k most typical implementation of some functionality in a set of
   * similar implementations of that functionality. It uses 0.3 as a default
   * bandwidth parameter.
   *
   * @param topK k most typical/representative source objects, where k cannot
   *             be greater than the size of the sources set.
   * @param h smoothing factor
   * @param resultSet a set of source objects implementing a similar functionality.
   * @param relevant relevant methods names to introspect
   * @return a new list of k most typical source objects implementing a similar functionality.
   */
  default List<Source> typicalityQuery(int topK, double h, Set<Source> resultSet, Set<String> relevant){
    return typicalityQuery(resultSet, new SimpleTypicalityProcessor(topK, h, relevant));
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
   * @param queryProcessor typicality query's processor.
   * @return a new list of the most typical source code implementing a functionality.
   * @see {@code https://www.cs.sfu.ca/~jpei/publications/typicality-vldb07.pdf}
   */
  default List<Source> typicalityQuery(Set<Source> resultSet, TypicalityProcessor queryProcessor){
    return queryProcessor.process(resultSet);
  }

  static UnitLocation locatedMethod(Context context, final Set<String> relevant){
    return context.locateMethods().stream()
      .filter(m -> relevant.contains(methodName(m.getUnitNode())))
      .findFirst()
      .orElse(null);
  }

  static UnitLocation locatedCompilationUnit(Context context){
    try {
      return new ProgramUnitLocation(
        context.getCompilationUnit(),
        Locations.locate(context.getCompilationUnit())
      );
    } catch (Exception e){
      System.out.println("Ignoring a package.java class");
      return null;
    }
  }

  static UnitLocation locateUnit(Source code, Set<String> relevant){
    final Context context = Sources.from(code);

    return (relevant.isEmpty()
      ? locatedCompilationUnit(context)
      : locatedMethod(context, relevant)
    );
  }

  /**
   * Pulls the code some method of interest.
   *
   * @param code source file
   * @param relevant relevant method names
   * @return the method snippet
   */
  static String methodCode(Source code, Set<String> relevant){
    final UnitLocation located = locateUnit(code, relevant);
    final SourceSelection selection = new SourceSelection(ImmutableList.of(located));
    return selection.toCode();
  }

  /**
   /**
   * Pulls the code of some segments of interest.
   *
   * @param code source file
   * @param relevant relevant method names
   * @return the segments snippet.
   */
  static String segmentsCode(Source code, Set<String> relevant){
    final UnitLocation unit   = locateUnit(code, relevant);

    if(Objects.isNull(unit)) // returns nothing
      return "";

    final Set<Location> whiteSet = Segments.relevantLocations(unit);
    final SourceSelection selection = new SourceSelection(whiteSet);

    return selection.toCode();
  }

  static float score(Source a, Source b, Map<Source, String> summaries){
    return similarityScore(
      summaries.get(a), summaries.get(b)
    );
  }


  /**
   * Introspection processor.
   *
   * @param <T> feature type
   * @param <R> return type
   */
  interface Processor <T, R> {

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
     * @param sources a set of source objects implementing a similar functionality.
     * @return a type R.
     */
    R process(Set<Source> sources);
  }


  interface TypicalityProcessor extends Processor <Snapshot, List<Source>> {
    /**
     * @return k most typical value.
     */
    int topK();

    /**
     * Generates a ranked (by typicality) list of source files.
     *
     * @param scoreboard existing typicality scores.
     * @return a new ranked list
     */
    default List<Source> rankedList(Map<Snapshot, Double> scoreboard) {
      final Map<Snapshot, Double> T = Objects.requireNonNull(scoreboard);

      final List<Snapshot> result = T.keySet().stream()
        .sorted((a, b) -> Double.compare(T.get(b), T.get(a)))
        .limit(topK())
        .collect(toList());

      return result.stream()
        .map(Snapshot::source)
        .collect(toList());
    }
  }


  /**
   * The feature type. A feature is a characteristic relevant to the typicality
   * analysis process.
   *
   * @param <T> parameter type representing the actual type of data held by this feature.
   */
  interface Feature <T> {
    /**
     * @return feature's data.
     */
    T data();
  }

  /**
   * A summary of the Source file as a feature.
   */
  class Snapshot implements Feature <String> {
    final Source code;
    final String data;

    Snapshot(Source code, String data){
      this.code   = code;
      this.data   = data;
    }
    /**
     * @return original source of data.
     */
    public Source source(){
      return code;
    }

    @Override public String data() {
      return data;
    }
  }


  /**
   * Default implementation of typicality analysis.
   */
  class SimpleTypicalityProcessor implements TypicalityProcessor {
    private final int         topK;
    private final double      h;
    private final Set<String> relevant;


    /**
     * Construct a new Content-based Typicality Processor
     *
     * @param h smoothing factor
     * @param relevant relevant method names
     */
    SimpleTypicalityProcessor(int topK, double h, Set<String> relevant){
      this.topK     = topK;
      this.h        = h;
      this.relevant = relevant;
    }

    @Override public Snapshot from(Source source) {
      return new Snapshot(source, segmentsCode(source, relevant));
    }

    @Override public List<Source> process(Set<Source> sources){
      if(sources.isEmpty()) return ImmutableList.of();
      if(topK() <= 0)       return ImmutableList.of();

      final Map<Snapshot, Double> T = new HashMap<>();
      final Set<Snapshot> features  = from(sources);

      // Compute the cartesian product of the sources object
      final Set<List<Snapshot>> cartesian = Sets.cartesianProduct(
        Arrays.asList(features, features)
      );


      // Initialize these features' scores
      for(Snapshot code : features){
        T.put(code, 0.0);
      }

      assert T.keySet().size() == sources.size();

      double t1  = 1.0d / (T.keySet().size() - 1) * Math.sqrt(2.0 * Math.PI);
      double t2  = 2.0 * Math.pow(h, 2);

      for(List<Snapshot> each : cartesian){
        final Snapshot oi = each.get(0);
        final Snapshot oj = each.get(1);

        double w = gaussianKernel(t1, t2, oi, oj);
        double Toi = T.get(oi) + w;
        double Toj = T.get(oj) + w;

        T.put(oi, Toi);
        T.put(oj, Toj);
      }

      return rankedList(T);
    }


    static double gaussianKernel(double t1, double t2, Snapshot oi, Snapshot oj){
      return t1 * Math.exp(-(Math.pow(score(oi, oj), 2) / t2));
    }

    static double score(Feature<String> a, Feature<String> b){
      return similarityScore(a.data(), b.data());
    }

    @Override public int topK() {
      return topK;
    }

    @Override public String toString() {
      return "SimpleTypicalityProcessor (smoothingFactor = " + h + ")";
    }
  }
}
