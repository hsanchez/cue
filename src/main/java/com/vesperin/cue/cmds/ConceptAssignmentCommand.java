package com.vesperin.cue.cmds;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vesperin.base.Source;
import com.vesperin.cue.BasicCli;
import com.vesperin.cue.utils.IO;
import com.vesperin.cue.utils.Sources;
import com.vesperin.text.Grouping;
import com.vesperin.text.Grouping.Group;
import com.vesperin.text.Grouping.Groups;
import com.vesperin.text.Index;
import com.vesperin.text.Query;
import com.vesperin.text.Query.Result;
import com.vesperin.text.Selection;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import static com.vesperin.text.Selection.Document;
import static com.vesperin.text.Selection.selects;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * @author Huascar Sanchez
 */
@Command(name = "concepts", description = "Recognizing implied concepts in code")
@SuppressWarnings("FieldCanBeLocal")
public class ConceptAssignmentCommand implements BasicCli.CliCommand {

  private static final String MAP_SET_CLUSTERS_NAME  = "clusters.txt";

  @Inject HelpOption<TypicalityAnalysisCommand> help;

  @Option(name = {"-d", "--directory"}, arity = 1, description = "extracts concepts from target directory.")
  private String directory = null;

  @Option(name = {"-k", "--topk"}, description = "k most typical source code.")
  private int topK = 10;

  @Option(name = {"-f", "--from"}, arity = 1, description = "focus in entries in target file.")
  private String from = null;

  @Option(name = {"-c", "--cap"}, description = "max number of elements in each group (default value: 36).")
  private int cap  = 36;

  @Option(name = {"-s", "--scope"}, description = "search scope: 0 -> classname (default); 1 -> method name; 2 -> method body.")
  private int scope = 0;

  @Option(name = {"-o", "--onscreen"}, description = "print results on screen.")
  private boolean onScreen = false;

  @Option(name = {"-m", "--map"}, description = "create [types]->[words] mappings.")
  private boolean map = false;


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

      int realCap;
      if(cap < 0){ realCap = 36; } else {
        realCap = Math.min(cap, 36/*selected value based on trials*/);
      }

      int realScope;
      if(scope < 0 || scope > 2) { realScope = 0; } else {
        realScope = scope;
      }

      return conceptAssignment(directory, from, topK, realCap, realScope, onScreen, map);
    }

    return 0;
  }

  private static int conceptAssignment(String target, String from, int topK, int cap, int scope, boolean onScreen, boolean map) {

    final List<Source> corpus = new ArrayList<>();
    final Stopwatch stopwatch = Stopwatch.createStarted();

    try {
      // check if method file was given
      if(from != null){
        final Path         methods  = Paths.get(from);
        final List<String> allLines = IO.readLines(methods);

        final Set<Source> corpusSet = corpus.stream().collect(Collectors.toSet());

        final Set<String> relevantSet = Sources.populate(corpus, allLines);

        final Selection.WordCollection wc = buildWordCollection(scope, relevantSet, StopWords.all());
        final List<Word> wordList = selects(topK, corpusSet, wc);

        System.out.println("[INFO]: Collected " + wordList.size() + " words: " + stopwatch);

        if(onScreen){
          System.out.println(wordList);
          System.out.println("[INFO]: Printed words: " + stopwatch);
        } else {
          System.out.println("Results will be displayed only if onscreen option is set to true.");
        }

      } else if (target != null){
        final Path start = Paths.get(target);
        corpus.addAll(Sources.from(IO.collectFiles(start, "java", "Test", "test", "package-info")));

        final Set<Source> corpusSet = corpus.stream().collect(Collectors.toSet());

        System.out.println("[INFO]: Read " + corpus.size() + " files: " + stopwatch);

        if(topK <= 0) {

          List<Word> words = selects(
            corpusSet,
            wordCollection(scope, Collections.emptySet(), StopWords.all())
          );

          System.out.println("[INFO]: Collected " + words.size() + " words: " + stopwatch);

          if(onScreen){
            System.out.println(words);
            System.out.println("[INFO]: Printed words: " + stopwatch);
          } else {
            System.out.println("Results will be displayed only if onscreen option is set to true.");
          }

        } else {

          final Set<StopWords> SW = updatedStopWords(target);
          System.out.println("[INFO]: Updated stopwords:  " + stopwatch);

          final List<Word> words = selects(corpusSet, Selection.inspectClassName(SW)).stream()
            .filter(w -> !StopWords.isStopWord(SW, w.element()))
            .collect(Collectors.toList());

          System.out.println("[INFO]: Selected relevant words:  " + stopwatch);

          if(!map){

            if(onScreen){
              System.out.println(words);
              System.out.println("[INFO]: Printed words: " + stopwatch);
            } else {
              System.out.println("Results will be displayed only if onscreen option is set to true.");
            }

          } else {
            final Groups  groups = Grouping.formDocGroups(words);
            final Index   index  = groups.index();
            System.out.println("[INFO]: Formed groups-of-types based on relevant words:  " + stopwatch);

            final List<Mapping> mappings = new ArrayList<>();

            for(Group eachGroup : groups){
              final Groups regroups = Grouping.formDocGroups(eachGroup, cap);
              for(Group regroup : regroups){
                final List<Document> ds = Group.items(regroup, Document.class);
                final Result r = Query.types(ds, index);
                final List<String> resultSet = Result.items(r, Word.class).stream().map(Word::element).collect(Collectors.toList());
                final List<String> names     = Document.names(ds);

                System.out.print(".");
                mappings.add(new Mapping(names, resultSet));
              }
            }

            System.out.println();
            System.out.println("[INFO]: Tuned the created groups that exceeded the " + cap + " size limit: " + stopwatch);

            final ResultPackage resultPackage = new ResultPackage(mappings);


            if(onScreen){
              final Gson gson = new GsonBuilder().setPrettyPrinting().create();
              System.out.println(gson.toJson(resultPackage));
              System.out.println("[INFO]: Printed file " + MAP_SET_CLUSTERS_NAME + " to screen: " + stopwatch);
            } else {
              final Gson gson = new Gson();
              Path newFile = Paths.get(MAP_SET_CLUSTERS_NAME);
              Files.deleteIfExists(newFile);

              Files.write(
                newFile,
                gson.toJson(resultPackage).getBytes(),
                CREATE,
                APPEND
              );

              System.out.println("[INFO]: Created/Updated file " + MAP_SET_CLUSTERS_NAME + ": " + stopwatch);

            }
          }



          System.out.println("[INFO]  total elapsed time: " + stopwatch.stop());

        }
      } else {
        System.err.println("Unable to parse your input!");
        return -1;
      }
    } catch (Exception e){
      e.printStackTrace(System.err);
      return -1;
    }

    return 0;
  }

  private static Set<StopWords> updatedStopWords(String target){
    final List<String> general = generalUpdate(target);
    final List<String> java    = Arrays.asList("scala", "get", "max", "message", "buffered");

    return StopWords.update(Collections.emptyList(), java, general);
  }

  private static Selection.WordCollection buildWordCollection(int scope, Set<String> relevant, Set<StopWords> stopWords){
    switch (scope){
      case 0:

        return relevant.isEmpty()
          ? Selection.inspectClassName(stopWords)
          : Selection.inspectClassName(relevant, stopWords);

      case 1:

        return relevant.isEmpty()
          ? Selection.inspectMethodName(stopWords)
          : Selection.inspectMethodName(relevant, stopWords);

      case 2:

        return relevant.isEmpty()
          ? Selection.inspectMethodBody(stopWords)
          : Selection.inspectMethodBody(relevant, stopWords);

      default: throw new NoSuchElementException("Could not recognize scope option " + scope);
    }
  }

  private static Selection.WordCollection wordCollection(int scope, Set<Word> relevant, Set<StopWords> stopWords){
    return buildWordCollection(scope, relevant.stream()
      .map(Word::element)
      .collect(Collectors.toSet()), stopWords);
  }

  private static List<String> generalUpdate(String target){
    return Arrays.asList(Paths.get(target).getFileName().toString(),
      "released", "initializers",
      "frustum", "automated", "per",
      "stored", "zero", "qualified",
      "cleanup", "normalize", "simple",
      "value", "threading", "destructor",
      "damping", "joystick", "matrixf",
      "aassert", "technique", "iassert",
      "universally", "anim", "mutex", "minkowski",
      "heightfield", "ragdoll", "influencer",
      "stat", "dummy", "touch", "probably",
      "profiler","stat","pass","compact","dyn"
      ,"mikk","fly","criterium","broad","xy"
      ,"kerning","csetjmp","ring","ye","fbo"
      ,"daabbc","mixer","guice","bayazit","cmath"
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
      this.types = types;
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


}
