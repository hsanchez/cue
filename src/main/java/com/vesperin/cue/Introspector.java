package com.vesperin.cue;

import Jama.Matrix;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.vesperin.base.Context;
import com.vesperin.base.Source;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.locators.ProgramUnitLocation;
import com.vesperin.base.locators.UnitLocation;
import com.vesperin.cue.segment.Segments;
import com.vesperin.cue.spi.Cluster;
import com.vesperin.cue.spi.Kmeans;
import com.vesperin.cue.spi.SourceSelection;
import com.vesperin.cue.spi.Words;
import com.vesperin.cue.text.Word;
import com.vesperin.cue.text.WordCounter;
import com.vesperin.cue.text.WordVisitor;
import com.vesperin.cue.utils.AstUtils;
import com.vesperin.cue.utils.Jamas;
import com.vesperin.cue.utils.Similarity;
import com.vesperin.cue.utils.Sources;
import com.vesperin.cue.utils.Threads;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.vesperin.cue.utils.AstUtils.methodName;
import static com.vesperin.cue.utils.Similarity.similarityScore;
import static java.util.stream.Collectors.toList;

/**
 * @author Huascar Sanchez
 */
public interface Introspector {

  AtomicInteger COUNTER = new AtomicInteger(0);

  /**
   * Determine the concepts (capped to 10 suggestions) that appear in a list of
   * sources.
   *
   * @param sources list of sources to inspect.
   * @return a new list of guessed concepts.
   */
  default List<Word> assignedConcepts(List<Source> sources){
    return assignedConcepts(Integer.MAX_VALUE, sources);
  }

  /**
   * Determine the concepts that appear in a list of
   * sources.
   *
   * @param topK k most frequent concepts in the list of sources.
   * @param sources list of sources to inspect.
   * @return a new list of guessed concepts.
   */
  default List<Word> assignedConcepts(int topK, List<Source> sources){
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
  default List<Word> assignedConcepts(int topK, List<Source> sources, final Set<String> relevantSet){

    final ExecutorService service = Threads.newExecutorService(sources.size()/topK);
    final WordCounter     counter = new WordCounter();

    final Map<Source, List<Word>> documentToWordList = new ConcurrentHashMap<>();
    final Set<Word> wordSet = ConcurrentHashMap.newKeySet();

    // 1. build document to word-list map
    for(final Source each : sources){
      if(each.getName().equals("package-info")){
        continue;
      }

      service.execute(
        () -> {
          final List<Word> wordList = assignedConcepts(each, relevantSet);
          documentToWordList.put(each, wordList);
          wordSet.addAll(wordList);
          counter.addAll(wordList);
        }
      );
    }

    // shuts down the executor service
    Threads.shutdownExecutorService(service);

    final Map<Integer,Word> wordIdValueMap = new HashMap<>();

    // 2. create a Map of ids to words from the wordSet
    int wordId = 0;
    for (Word word : wordSet) {
      wordIdValueMap.put(wordId, word);
      wordId++;
    }

    // we need a documents.keySet().size() x wordSet.size() matrix to hold
    // this info
    final int         numDocs   = sources.size();
    final int         numWords  = wordSet.size();
    final double[][]  data      = new double[numWords][numDocs];

    for (int i = 0; i < numWords; i++) {
      for (int j = 0; j < numDocs; j++) {

        final List<Word> ws = documentToWordList.get(sources.get(j));
        if(ws == null) continue;

        final Word word = wordIdValueMap.get(i);

        int count = 0; for(Word each : ws){
          if(Objects.equals(each, word)) count++;
        }

        data[i][j] = count;
      }
    }

    final List<String> documents = sources.stream().map(Source::getName).collect(toList());
    final List<Word>   wordList  = wordSet.stream().collect(toList());

//
//    System.out.println("\n\n\n");
//    System.out.println("BEGIN: WORDS");
//    wordList.forEach(e -> System.out.print(e.getWord() + " "));
//    System.out.println("END: WORDS");

    System.out.println("\n\n\n");

    final Matrix raw    = new Matrix(data);
    Jamas.printMatrix("RAW", raw, documents, wordList, new PrintWriter(System.out));

    // Turns tf-idf statistic into a score (to be used as word ranking)
    final Matrix tfidf = Jamas.tfidfMatrix(raw);

    System.out.println(String.format("[INFO] matrix %d x %d", tfidf.getRowDimension(), tfidf.getColumnDimension()));
    System.out.println("[INFO] # methods inspected? " + COUNTER.get());

    return topKWord(topK, tfidf, wordList);
  }


  default List<Word> topKWord(int k, Matrix matrix, List<Word> wordList){

    final Map<Word, Double> scores = new HashMap<>();
    for (int i = 0; i < matrix.getRowDimension(); i++) {
      final double s = Jamas.rowSum(matrix, i);
      scores.put(wordList.get(i), s);
    }

    return scores.entrySet().stream()
      .sorted((a, b) -> Doubles.compare(b.getValue(), a.getValue()))
      .limit(k).map(Map.Entry::getKey).collect(toList());
  }


  default List<Cluster> clusters(List<Word> topWords, List<Source> sources){


    System.out.println("\n\n\n");
    System.out.println("BEGIN: WORDS");
    topWords.forEach(e -> System.out.print(e.getWord() + " "));
    System.out.println("END: WORDS");

    final Map<Source, List<Word>> documentToWordList = new ConcurrentHashMap<>();
    final List<Source> corpus = Lists.newArrayList();

    for( Source src : sources){
      for(Word w : topWords){
        final String lowerCaseContent = src.getContent().toLowerCase(Locale.ENGLISH);
        if(lowerCaseContent.contains(w.getWord())){
          if(documentToWordList.containsKey(src)){
            documentToWordList.get(src).add(w);
            corpus.add(src);
          } else {
            documentToWordList.put(src, Lists.newArrayList(w));
            corpus.add(src);
          }
        }
      }
    }

    final Map<Integer,Word> wordIdValueMap = new HashMap<>();

    // 2. create a Map of ids to words from the wordSet
    int wordId = 0;
    for (Word word : topWords) {
      wordIdValueMap.put(wordId, word);
      wordId++;
    }

    final int         numDocs   = documentToWordList.keySet().size();
    final int         numWords  = topWords.size();
    final double[][]  data      = new double[numWords][numDocs];

    for (int i = 0; i < numWords; i++) {
      for (int j = 0; j < numDocs; j++) {

        final List<Word> ws = documentToWordList.get(corpus.get(j));
        final Word word = wordIdValueMap.get(i);

        int count = 0; for(Word each : ws){
          if(Objects.equals(each, word)) count++;
        }

        data[i][j] = count;
      }
    }

    final List<String> documents = documentToWordList.keySet().stream()
      .map(Source::getName).collect(toList());

    final Matrix rawMatrix = new Matrix(data);
    Jamas.printRawFreqMatrix(rawMatrix, documents, topWords);

    final Matrix lsiMatrix = Jamas.runLSI(rawMatrix);
    Jamas.printMatrix("LSI matrix", lsiMatrix, documents, topWords, new PrintWriter(System.out));

    final Words words = new Words(lsiMatrix, topWords);

    return Kmeans.cluster(words);
  }


  /**
   * Determine the concepts (capped to 10 suggestions) that appear in a given source code.
   *
   * @param code source code to introspect.
   * @return a new list of guessed concepts.
   */
  default List<Word> assignedConcepts(Source code){
    return assignedConcepts(code, ImmutableSet.of());
  }


  /**
   * Determine the concepts (capped to 10 suggestions) that appear in a given source code.
   *
   * @param code source code to introspect.
   * @param relevant set of relevant method names. At least one match should exist.
   * @return a new list of guessed concepts.
   */
  default List<Word> assignedConcepts(Source code, final Set<String> relevant){
    final Context   context = Sources.from(code);

    final UnitLocation unit = relevant.isEmpty()
      ? locatedCompilationUnit(context)
      : locatedMethod(context, relevant);

    if(unit == null) return ImmutableList.of();

    COUNTER.addAndGet(AstUtils.methodCount(unit.getUnitNode()));

    return assignedConcepts(unit, 10);
  }

  /**
   * Determine the top k most concepts that appear within a located unit.
   *
   * @param locatedUnit list of sources to inspect.
   * @param topK k most frequent concepts in the list of sources.
   * @return a new list of guessed concepts.
   */
  default List<Word> assignedConcepts(Location locatedUnit, int topK){
    final UnitLocation  unitLocation  = (UnitLocation) Objects.requireNonNull(locatedUnit);
    final Stopwatch watch = Stopwatch.createStarted();
    final Set<Location> irrelevantSet = unitLocation.getUnitNode() instanceof CompilationUnit ?
      new HashSet<>() : Segments.irrelevantLocations(unitLocation);
    System.out.println(String.format("Find irrelevant locations (%d): ", irrelevantSet.size()) + watch);

    return interestingConcepts(topK, unitLocation, irrelevantSet);
  }

  /**
   * Determine the top k most interesting concepts that appear within a located unit.
   *
   * @param topK k most frequent concepts in the list of sources.
   * @param located located unit.
   * @param irrelevantSet set of irrelevant names
   * @return a new list of interesting concepts.
   */
  default List<Word> interestingConcepts(int topK, UnitLocation located,
          Set<Location> irrelevantSet){
    // collect frequent words outside the blacklist of locations
    Stopwatch stopwatch = Stopwatch.createStarted();

    System.out.println("#interestingConcepts: Warming up " + located.getSource().getName());

    if(located.getSource().getName().equals("DynamicsWorld")){
      System.out.println(located);
    }

    final WordVisitor extractor   = new WordVisitor(irrelevantSet);
    located.getUnitNode().accept(extractor);

    System.out.println("#interestingConcepts: Started counting " + located.getSource().getName() + ": " + stopwatch);

    final List<Word> words = extractor.getItems();
    System.out.println(String.format("Finished counting with %s : #%s words", located.getSource().getName(), words.size()));
    final WordCounter wordCounter = new WordCounter(words);

    System.out.println("#interestingConcepts: Count words " + stopwatch);

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
  default List<Source> representativeTypicalityQuery(Set<Source> resultSet, Set<String> domain){
    return representativeTypicalityQuery(5/*optimization: 5 most typical are enough*/, resultSet, domain);
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
  default List<Source> representativeTypicalityQuery(int topk, Set<Source> resultSet, Set<String> domain) {
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
    return Similarity.similarityScore(
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
   * Coverage relation between typical source file and non typical source files.
   */
  class Coverage implements Feature <Map<Source, List<Source>>> {
    final Map<Source, List<Source>> region;

    Coverage(){
      this.region = new HashMap<>();
    }

    /**
     * Adds coverage between source files.
     *
     * @param thisCode object to be covered by the other object.
     * @param toThatCode
     */
    public void adds(Source thisCode, Source toThatCode){
      if(!region.containsKey(toThatCode)){
        region.put(toThatCode, Lists.newArrayList(thisCode));
      } else {
        region.get(toThatCode).add(thisCode);
      }
    }

    @Override public Map<Source, List<Source>> data() {
      return data();
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
