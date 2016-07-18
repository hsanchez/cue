package com.vesperin.cue.cmds;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.vesperin.base.Source;
import com.vesperin.cue.Cue;
import com.vesperin.cue.Introspector;
import com.vesperin.cue.IntrospectorWithCli;
import com.vesperin.cue.spi.Cluster;
import com.vesperin.cue.text.StopWords;
import com.vesperin.cue.text.Word;
import com.vesperin.cue.utils.IO;
import com.vesperin.cue.utils.Sources;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
@Command(name = "concepts", description = "Recognizing implied concepts in code")
public class ConceptAssignmentCommand implements IntrospectorWithCli.CliCommand {

  @Inject HelpOption<TypicalityAnalysisCommand> help;

  @Option(name = {"-d", "--directory"}, arity = 1, description = "extracts concepts from target directory.")
  private String directory = null;

  @SuppressWarnings("FieldCanBeLocal")
  @Option(name = {"-k", "--topk"}, description = "k most typical source code.")
  private int topK = 10;

  @Option(name = {"-f", "--from"}, arity = 1, description = "focus in entries in target file.")
  private String from = null;


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

      return conceptAssignment(directory, from, topK);
    }

    return 0;
  }

  private static int conceptAssignment(String target, String from, int topK) {

    final List<Source> corpus = new ArrayList<>();
    final Introspector cue    = Cue.newIntrospector();

    try {
      // check if method file was given
      if(from != null){
        final Path         methods  = Paths.get(from);
        final List<String> allLines = IO.readLines(methods);

        final Set<String> relevantSet = Sources.populate(corpus, allLines);
        System.out.println(cue.assignedConcepts(topK, corpus, relevantSet));

      } else if (target != null){
        final Path start = Paths.get(target);
        corpus.addAll(Sources.from(IO.collectFiles(start, "java", "Test", "test", "package-info")));

        System.out.println("[INFO] Number of files in corpus? " + corpus.size());

        if(topK <= 0) {
          System.out.println(cue.assignedConcepts(corpus));
        } else {
          StopWords.JAVA.add(Paths.get(target).getFileName().toString());

          // the entire body declaration (e.g., all methods) is relevant
//          final Set<Source> atLeast8  = new HashSet<>();
//          final Set<Source> atLeast5  = new HashSet<>();
//          final Set<Source> rest      = new HashSet<>();
//          final Set<String> terms     = cue.assignedConcepts(topK, corpus).stream().collect(Collectors.toSet());
//
//          for(Source each : corpus){
//            final Context context = Sources.from(each);
//            final WordVisitor iterator = new WordVisitor();
//            context.accept(iterator);
//
//            final WordCounter counter = new WordCounter(iterator.getItems());
//            final Set<String> most    = counter.mostFrequent(topK).stream().collect(Collectors.toSet());
//
//            final int hit = Sets.intersection(terms, most).size();
//            if(hit >= 8){
//              atLeast8.add(each);
//            } else if (hit >= 5 && hit < 8){
//              atLeast5.add(each);
//            } else {
//              rest.add(each);
//            }
//          }
//
//
//          System.out.println(atLeast8.stream().map(Source::getName).collect(Collectors.toList()));
//          System.out.println(atLeast5.stream().map(Source::getName).collect(Collectors.toList()));
//          System.out.println(rest.stream().map(Source::getName).collect(Collectors.toList()));
//

          final List<Word> words = cue.assignedConcepts(topK, corpus);
          System.out.println(words);

          final List<Cluster> recommend = cue.clusters(words, corpus);

          System.out.println("========================");
          System.out.println("Clusters of semantically similar words:");
          for(Cluster each : recommend){
            System.out.println(each.getWords());
          }

          System.out.println("========================");

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
}
