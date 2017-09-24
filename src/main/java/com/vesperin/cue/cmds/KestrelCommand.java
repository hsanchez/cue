package com.vesperin.cue.cmds;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.vesperin.cue.BasicCli;
import com.vesperin.text.Corpus;
import com.vesperin.text.Grouping;
import com.vesperin.text.Introspector;
import com.vesperin.text.Recommend;
import com.vesperin.text.Selection;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.spi.BasicExecutionMonitor;
import com.vesperin.text.spi.ExecutionMonitor;
import com.vesperin.text.tokenizers.Tokenizers;
import com.vesperin.text.tokenizers.WordsTokenizer;
import com.vesperin.text.utils.Prints;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * @author Huascar Sanchez
 */
@Command(name = "kestrel", description = "Kestrel data analysis")
@SuppressWarnings("FieldCanBeLocal")
public class KestrelCommand implements BasicCli.CliCommand {

  private static final String CLUSTERS_FILE  = "clusters.json";
  private static final String WORDS_FILE     = "words.json";

  @Inject HelpOption<TypicalityAnalysisCommand> help;

  @Option(name = {"-c", "--cluster"}, description = "Clusters those types matching collected words.")
  private boolean cluster  = false;

  @Option(name = {"-f", "--from"}, arity = 1, description = "File with source file locations from where concepts will be extracted.")
  private String from = null;

  @Option(name = {"-v", "--verbose"}, description = "Prints debugging messages")
  private boolean verbose = false;

  @Option(name = {"-k", "--topk"}, description = "Number of words to collect")
  private int topK = 10;

  @Override public Integer call() throws Exception {
    final ExecutionMonitor monitor = BasicExecutionMonitor.get();
    final Stopwatch stopwatch = Stopwatch.createStarted();
    if(!help.showHelpIfRequested()){

      if(Objects.isNull(from)) {
        System.err.println("You must specify a json file as input (see -help for information).");
        return -1;
      }

      if(topK < 1){
        System.err.println("You must specify a valid k number (see -help for information).");
        return -1;
      }

      if(verbose){ BasicExecutionMonitor.get().enable(); } else {
        BasicExecutionMonitor.get().disable();
      }

      final Corpus<String> corpus     = generateCorpus(from);
      final WordsTokenizer tokenizer  = Tokenizers.tokenizeString(StopWords.all());

      if(cluster){

        final Grouping.Groups groups    = Introspector.partitionCorpus(corpus, tokenizer);
        monitor.info("formed groups of classnames: " + stopwatch);

        final List<Record> records = new ArrayList<>();
        for(Grouping.Group each : groups){
          final List<Selection.Document>  ds    = Grouping.Group.items(each, Selection.Document.class);
          final String          label = Recommend.coalesce(Recommend.labels(ds));

          final List<String> names      = Selection.Document.containers(ds);
          final List<String> singleton  = Collections.singletonList(label);

          records.add(new Record(names, singleton));
        }

        monitor.info("collected groups of classnames: " + stopwatch);

        final RecordPackage resultPackage = new RecordPackage(records);
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if(monitor.isActive()){ // print to screen
          monitor.info(gson.toJson(resultPackage));
          monitor.info("Printed file " + "clusters.json" + " to screen: " + stopwatch);
        } else {
          Path newFile = Paths.get("clusters.json");
          Files.deleteIfExists(newFile);

          Files.write(
            newFile,
            gson.toJson(resultPackage).getBytes(),
            CREATE,
            APPEND
          );

          monitor.info("Created/Updated file " + "clusters.json" + ": " + stopwatch);
        }

      } else {
        return wordCollection(topK, corpus, tokenizer);
      }


    }

    stopwatch.stop();
    return 0;
  }

  private static int wordCollection(int topK, Corpus<String> corpusObject, WordsTokenizer tokenizer){
    final ExecutionMonitor monitor = BasicExecutionMonitor.get();
    final Stopwatch stopwatch = Stopwatch.createStarted();

    try {

      final Map<List<Selection.Word>, List<Selection.Word>> relevantWords = Introspector.buildWordsMap(corpusObject, tokenizer);
      if(relevantWords.isEmpty()) {
        monitor.info("No words to report!");
      } else {

        final int keySetSize = Iterables.get(relevantWords.keySet(), 0).size();
        final List<String> frequentOnes       = Iterables.get(relevantWords.keySet(), 0).stream()
          .sorted((a, b) -> Integer.compare(b.count(), a.count()))
          .limit(Math.min(topK, keySetSize))
          .map(Selection.Word::element)
          .collect(Collectors.toList());


        final int valueSize = Iterables.get(relevantWords.values(), 0).size();
        final List<String> typicalOnes = Iterables.get(relevantWords.values(), 0).stream()
          .limit(Math.min(topK, valueSize))
          .map(Selection.Word::element)
          .collect(Collectors.toList());


        List<Selection.Word> representativeOnes = Introspector.representativeWords(relevantWords);
        final int representativeLimit = Math.min(topK, representativeOnes.size());
        final List<String> reprOnes = representativeOnes.stream()
          .limit(representativeLimit)
          .map(Selection.Word::element)
          .collect(Collectors.toList());


        if(monitor.isActive()){
          monitor.info("Frequent words " + Prints.toPrettyPrintedList(frequentOnes, false));
          monitor.info("Typical words " + Prints.toPrettyPrintedList(typicalOnes, false));
          monitor.info("Representative words " + Prints.toPrettyPrintedList(reprOnes, false));
        } else {
          final ConceptAssignmentCommand.WordPackage wordPackage = new ConceptAssignmentCommand.WordPackage(frequentOnes, typicalOnes, reprOnes);
          final Gson gson = new GsonBuilder().setPrettyPrinting().create();

          Path newFile = Paths.get("words.json");
          Files.deleteIfExists(newFile);

          Files.write(
            newFile,
            gson.toJson(wordPackage).getBytes(),
            CREATE,
            APPEND
          );

          monitor.info("Created/Updated file " + "words.json" + ": " + stopwatch);
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

  private static Corpus<String> generateCorpus(String from) throws FileNotFoundException {
    final Gson gson = new Gson();

    final JsonReader reader = new JsonReader(new FileReader(from));
    final Map<String, String> record = gson.fromJson(reader, Map.class);
    final List<String> classnames = record.values().stream().filter(s -> Character.isUpperCase(s.charAt(0)) && Character.isAlphabetic(s.charAt(0))).collect(Collectors.toList());

    final Corpus<String> corpus = Corpus.ofStrings();
    corpus.addAll(classnames);
    return corpus;
  }

  private final static class Record {
    final List<String> names;
    final List<String> label;

    Record(List<String> names, List<String> label){
      this.names = names;
      this.label = label;
    }

    public List<String> getNames() {
      return names;
    }

    public List<String> getLabel() {
      return label;
    }
  }

  private static class RecordPackage {
    List<Record> records;
    RecordPackage(List<Record> records){
      this.records = records;
    }

    public List<Record> getRecords() {
      return records;
    }

    public void setRecords(List<Record> records) {
      this.records = records;
    }
  }


}
