package com.vesperin.cue.cmds;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vesperin.base.Context;
import com.vesperin.base.Source;
import com.vesperin.cue.BasicCli;
import com.vesperin.cue.utils.IO;
import com.vesperin.cue.utils.Sources;
import com.vesperin.text.Corpus;
import com.vesperin.text.Grouping;
import com.vesperin.text.Grouping.Group;
import com.vesperin.text.Grouping.Groups;
import com.vesperin.text.Introspector;
import com.vesperin.text.Recommend;
import com.vesperin.text.Selection;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.spi.BasicExecutionMonitor;
import com.vesperin.text.spi.ExecutionMonitor;
import com.vesperin.text.tokenizers.Tokenizers;
import com.vesperin.text.tokenizers.WordsTokenizer;
import com.vesperin.text.utils.Prints;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.vesperin.text.Selection.Document;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * @author Huascar Sanchez
 */
@Command(name = "concepts", description = "Recognizing implied concepts in code")
@SuppressWarnings("FieldCanBeLocal")
public class ConceptAssignmentCommand implements BasicCli.CliCommand {

  static AtomicInteger counter = new AtomicInteger(0);

  private static final String MAP_SET_CLUSTERS_NAME  = "clusters.json";
  private static final String MAP_SET_WORDS_NAME     = "words.json";

  @Inject HelpOption<TypicalityAnalysisCommand> help;

  @Option(name = {"-d", "--directory"}, arity = 1, description = "Directory from where concepts will be extracted.")
  private String directory = null;

  @Option(name = {"-k", "--topk"}, description = "Number of words to collect")
  private int topK = 10;

  @Option(name = {"-f", "--from"}, arity = 1, description = "File with source file locations from where concepts will be extracted.")
  private String from = null;

  @Option(name = {"-c", "--cluster"}, description = "Clusters those types matching collected words.")
  private boolean cluster  = false;

  @Option(name = {"-e", "--element"}, description = "Element to introspect: 0 -> classname (default); 1 -> method name; 2 -> method body.")
  private int element = 0;

  @Option(name = {"-v", "--verbose"}, description = "Prints debugging messages")
  private boolean verbose = false;

  @Option(name = {"-s", "--strategy"}, description = "Selects clustering strategy: 0 -> mst (default); 1 -> kmeans; 2 -> hybrid.")
  private int strategy = 1;

  @Option(name = {"-fn", "--file-names"}, description = "Lists FileName-and-qualifiedName mappings.")
  private boolean filenames  = false;


  @Override public Integer call() throws Exception {
    if(!help.showHelpIfRequested()){
      if(directory == null && from == null) {
        System.err.println("Please use a valid option (see -help for information).");
        return -1;
      }

      if(topK < 1) {
        System.err.println("Please use a valid topK value (see -help for information).");
        return -1;
      }

      int realScope;
      if(element < 0 || element > 2) { realScope = 0; } else {
        realScope = element;
      }

      if(verbose){ BasicExecutionMonitor.get().enable(); } else {
        BasicExecutionMonitor.get().disable();
      }

      int realStrategy;
      if(strategy < 0 || strategy > 2) { realStrategy = 0; } else {
        realStrategy = strategy;
      }

      final Map<Corpus<Source>, WordsTokenizer> record = generateRequiredObjects(realScope, from, directory);
      if(record.isEmpty()) return -1;
      final Corpus<Source> corpus     = Iterables.get(record.keySet(), 0);

      final WordsTokenizer tokenizer  = Iterables.get(record.values(), 0);


      // frequent, typical, and representative words
      if(!cluster){
        return wordCollection(topK, corpus, tokenizer);
      } else {
        return documentClustering(
          topK, realStrategy, corpus, tokenizer, filenames
        );
      }
    }

    return 0;
  }

  private static int documentClustering(int k, int clusteringStrategy, Corpus<Source> corpus, WordsTokenizer tokenizer, boolean filenames){

    if(filenames){
      try {
        final Set<Map<String, String>> data = new HashSet<>();

        for(Source each : corpus){
          final Context e = Selection.newContext(each);

          final ITypeBinding binding = ((AbstractTypeDeclaration)e.getCompilationUnit()
            .types()
            .get(0))
            .resolveBinding();


          if(binding == null){
            System.out.println("No type binding for " + each.getName());
            continue;
          }

          final Map<String, String> entry = new HashMap<>();
          entry.put(each.getName(), binding.getQualifiedName());

          data.add(entry);

        }

        final Gson gson = new GsonBuilder()
          .setPrettyPrinting()
          .create();

        System.out.println(gson.toJson(data));

        return 0;

      } catch (Exception e){
        e.printStackTrace(System.err);
        return -1;
      }

    }



    final Stopwatch stopwatch = Stopwatch.createStarted();
    final ExecutionMonitor monitor = BasicExecutionMonitor.get();

    try {

      final Groups  groups;

      switch (clusteringStrategy){
        case 0:
          monitor.info("Partitioning corpus with MST");
          groups = Introspector.partitionCorpus(k, corpus, tokenizer);
          break;
        case 1:
          monitor.info("Partitioning corpus with KMeans");
          final List<Word> frequentOnes = Selection.topKFrequentWords(k, corpus, tokenizer);
          groups = Grouping.groupDocsUsingWords(frequentOnes);
          break;
        case 2:
          monitor.info("Partitioning corpus with hybrid approach");
          groups = Grouping.groupDocs(corpus, tokenizer);
          break;
        default:
          monitor.error("Unknown clustering strategy", new NoSuchElementException());
          return -1;
      }

      monitor.info("Formed groups-of-types based on relevant words:  " + stopwatch);

      final List<Mapping> mappings = new ArrayList<>();

      //final Map<String, Set<String>> qualifyingMap = new HashMap<>();
      for(Group each : groups){
        final List<Document>  ds    = Group.items(each, Document.class);

        final boolean isNamedGroup  = each instanceof Grouping.NamedGroup;

        final String          label = isNamedGroup
          ? ((Grouping.NamedGroup) each).name()
          : Recommend.coalesce(Recommend.labels(ds));

        final List<String> names = new ArrayList<>();
        for(Document eachDoc : ds){
          names.add(eachDoc.toString() + "[" + eachDoc.getStart() + ", " + eachDoc.getEnd() + "]");
        }

        //final List<String> names      = Document.containers(ds);

        names.forEach(s -> System.out.println("\"" + s + "\","));
//        names.forEach(s -> {
//
//          int idx  = s.lastIndexOf('.');
//          int idx2 = s.lastIndexOf('[');
//          if(idx > 0 && idx2 > 0){
//
//            final String shortName = s.substring(idx + 1, idx2);
//            if(qualifyingMap.containsKey(shortName)){
//              qualifyingMap.get(shortName).add(s);
//            } else {
//              qualifyingMap.put(shortName, new HashSet<>(Collections.singletonList(s)));
//            }
//
//          }
//
//        });

        final List<String> singleton  = Collections.singletonList(label);

        mappings.add(new Mapping(names, singleton));
      }


//      final Gson gson0 = new GsonBuilder().setPrettyPrinting().create();
//      System.out.println("OK");
//      System.out.println(gson0.toJson(qualifyingMap));

      final ResultPackage resultPackage = new ResultPackage(mappings);
      final Gson gson = new GsonBuilder().setPrettyPrinting().create();

      System.out.println(String.format("INFO: Processed %d records. Expected %d records.", resultPackage.getMappings().size(), counter.get()));

      if(monitor.isActive()){ // print to screen
        monitor.info(gson.toJson(resultPackage));
        monitor.info("Printed file " + MAP_SET_CLUSTERS_NAME + " to screen: " + stopwatch);
      } else {
        Path newFile = Paths.get(MAP_SET_CLUSTERS_NAME);
        Files.deleteIfExists(newFile);

        Files.write(
          newFile,
          gson.toJson(resultPackage).getBytes(),
          CREATE,
          APPEND
        );

        monitor.info("Created/Updated file " + MAP_SET_CLUSTERS_NAME + ": " + stopwatch);
      }

    } catch (Exception e){
      e.printStackTrace(System.err);
      stopwatch.stop();
      return -1;
    }

    stopwatch.stop();
    return 0;
  }


  private static Map<Corpus<Source>, WordsTokenizer> generateRequiredObjects(int scope, String file, String directory){
    final ExecutionMonitor monitor = BasicExecutionMonitor.get();

    final Corpus<Source> corpusObject = Corpus.ofSources();
    final List<Source>   corpus       = new ArrayList<>();

    final Stopwatch stopwatch = Stopwatch.createStarted();

    final Set<StopWords> stopWords = updatedStopWords(directory);
    monitor.info("Updated stopwords:  " + stopwatch);

    WordsTokenizer tokenizer;

    try {

      if(!Objects.isNull(file) && !Objects.isNull(directory)) return Collections.emptyMap();

      if(!Objects.isNull(file)){

        final Path         methods      = Paths.get(file);
        final List<String> allLines     = IO.readLines(methods);

        final Set<String>  relevantSet  = Sources.populate(corpus, allLines);
        tokenizer    = buildWordTokenizer(scope, relevantSet, stopWords);

        corpusObject.addAll(corpus);

        counter.compareAndSet(counter.get(), corpusObject.size());

        return Collections.singletonMap(corpusObject, tokenizer);

      } else {


        final Path start = Paths.get(directory);
        corpus.addAll(Sources.from(IO.collectFiles(start, "java", "Test", "test", "package-info")));

        final Set<Source> corpusSet = new HashSet<>(corpus);
        corpusObject.addAll(corpusSet);

        counter.compareAndSet(counter.get(), corpusObject.size());

        tokenizer = wordTokenizer(scope, Collections.emptySet(), stopWords);

        return Collections.singletonMap(corpusObject, tokenizer);
      }


    } catch (Exception e){
      e.printStackTrace(System.err);
      return Collections.emptyMap();
    }
  }


  private static int wordCollection(int topK, Corpus<Source> corpusObject, WordsTokenizer tokenizer){
    final ExecutionMonitor monitor = BasicExecutionMonitor.get();
    final Stopwatch stopwatch = Stopwatch.createStarted();

    try {

      final Map<List<Word>, List<Word>> relevantWords = Introspector.buildWordsMap(corpusObject, tokenizer);
      if(relevantWords.isEmpty()) {
        monitor.info("No words to report!");
      } else {

        final int keySetSize = Iterables.get(relevantWords.keySet(), 0).size();
        final List<String> frequentOnes       = Iterables.get(relevantWords.keySet(), 0).stream()
          .sorted((a, b) -> Integer.compare(b.count(), a.count()))
          .limit(Math.min(topK, keySetSize))
          .map(Word::element)
          .collect(Collectors.toList());


        final int valueSize = Iterables.get(relevantWords.values(), 0).size();
        final List<String> typicalOnes = Iterables.get(relevantWords.values(), 0).stream()
          .limit(Math.min(topK, valueSize))
          .map(Word::element)
          .collect(Collectors.toList());


        List<Word> representativeOnes = Introspector.representativeWords(relevantWords);
        final int representativeLimit = Math.min(topK, representativeOnes.size());
        final List<String> reprOnes = representativeOnes.stream()
          .limit(representativeLimit)
          .map(Word::element)
          .collect(Collectors.toList());


        if(monitor.isActive()){
          monitor.info("Frequent words " + Prints.toPrettyPrintedList(frequentOnes, false));
          monitor.info("Typical words " + Prints.toPrettyPrintedList(typicalOnes, false));
          monitor.info("Representative words " + Prints.toPrettyPrintedList(reprOnes, false));
        } else {
          final WordPackage wordPackage = new WordPackage(frequentOnes, typicalOnes, reprOnes);
          final Gson gson = new GsonBuilder().setPrettyPrinting().create();

          Path newFile = Paths.get(MAP_SET_WORDS_NAME);
          Files.deleteIfExists(newFile);

          Files.write(
            newFile,
            gson.toJson(wordPackage).getBytes(),
            CREATE,
            APPEND
          );

          monitor.info("Created/Updated file " + MAP_SET_WORDS_NAME + ": " + stopwatch);
        }
      }

      stopwatch.stop();
      return 0;
    } catch (Exception e){
      e.printStackTrace(System.err);
      stopwatch.stop();
      return -1;
    }
  }

  private static Set<StopWords> updatedStopWords(String target){
    final List<String> general = generalUpdate(target);
    final List<String> java    = Arrays.asList("scala", "get", "max", "message", "buffered", "comparison");

    final Set<StopWords> t = StopWords.of(StopWords.JAVA, StopWords.GENERAL);

    return new HashSet<>(); //StopWords.update(Collections.emptyList(), java, Collections.emptyList());
  }

  private static WordsTokenizer buildWordTokenizer(int scope, Set<String> relevant, Set<StopWords> stopWords){
    switch (scope){
      case 0:

        System.out.println("ClassName Tokenizer");

        return relevant.isEmpty()
          ? Tokenizers.tokenizeTypeDeclarationName(stopWords)
          : Tokenizers.tokenizeTypeDeclarationName(relevant, stopWords);

      case 1:

        System.out.println("MethodName Tokenizer");

        return relevant.isEmpty()
          ? Tokenizers.tokenizeMethodDeclarationName(stopWords)
          : Tokenizers.tokenizeMethodDeclarationName(relevant, stopWords);

      case 2:

        System.out.println("MethodBody Tokenizer");

        return relevant.isEmpty()
          ? Tokenizers.tokenizeMethodDeclarationBody(stopWords)
          : Tokenizers.tokenizeMethodDeclarationBody(relevant, stopWords);

      default: throw new NoSuchElementException("Could not recognize scope option " + scope);
    }
  }

  private static WordsTokenizer wordTokenizer(int scope, Set<Word> relevant, Set<StopWords> stopWords){
    return buildWordTokenizer(scope, relevant.stream()
      .map(Word::element)
      .collect(Collectors.toSet()), stopWords);
  }

  private static List<String> generalUpdate(String target){
    return Arrays.asList(Paths.get(target).getFileName().toString(),
      "released", "initializers", "narrow",
      "frustum", "automated", "per",
      "stored", "zero", "qualified", "glsl",
      "cleanup", "normalize", "simple", "logic",
      "value", "threading", "destructor",
      "damping", "joystick", "matrixf", "app", "half",
      "aassert", "technique", "iassert",
      "universally", "anim", "mutex", "minkowski", "closest", "store",
      "heightfield", "ragdoll", "influencer", "generation",
      "stat", "dummy", "touch", "probably", "datum",
      "profiler","stat","pass","compact","dyn"
      ,"mikk","fly","criterium","broad","xy"
      ,"kerning","csetjmp","ring","ye","fbo"
      ,"daabbc","mixer","guice","bayazit","cmath", "constrained"
      ,"smart","dpu","daabb","cstdlib","fake","cstdio"
      ,"prefiltered","stopwatch","endien","quick","nano"
      ,"wrapped","cstdarg","statistic","billboard","ctype"
      ,"seekable","clonable","legacy","warburton","define"
      ,"working","placeholder","cstring","slouse"
      ,"performator","well", "libccd", "ye", "mikktspace", "debugger", "newtonian"
    );
  }

  private static class ResultPackage {
    List<Mapping> mappings;
    ResultPackage(List<Mapping> mappings){
      this.mappings = mappings;
    }

    public List<Mapping> getMappings() {
      return mappings;
    }

    public void setMappings(List<Mapping> mappings) {
      this.mappings = mappings;
    }
  }


  private static class Mapping {
    List<String> types;
    List<String> labels;

    Mapping(List<String> types, List<String> labels){
      this.labels = labels;
      this.types  = types;
    }

    public List<String> getTypes() {
      return types;
    }

    public void setTypes(List<String> types) {
      this.types = types;
    }

    public List<String> getLabels() {
      return labels;
    }

    public void setLabels(List<String> labels) {
      this.labels = labels;
    }
  }

  static class WordPackage {
    List<String> frequent;
    List<String> typical;
    List<String> representative;

    WordPackage(List<String> frequent, List<String> typical, List<String> representative){

      this.frequent       = frequent;
      this.typical        = typical;
      this.representative = representative;

    }

    public List<String> getFrequent() {
      return frequent;
    }

    public void setFrequent(List<String> frequent) {
      this.frequent = frequent;
    }

    public List<String> getTypical() {
      return typical;
    }

    public void setTypical(List<String> typical) {
      this.typical = typical;
    }

    public List<String> getRepresentative() {
      return representative;
    }

    public void setRepresentative(List<String> representative) {
      this.representative = representative;
    }
  }

}
