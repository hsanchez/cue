package com.vesperin.cue.cmds;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.vesperin.base.Source;
import com.vesperin.cue.IntrospectorWithCli;
import com.vesperin.cue.utils.IO;
import com.vesperin.cue.utils.Sources;
import com.vesperin.text.Grouping;
import com.vesperin.text.Grouping.Group;
import com.vesperin.text.Grouping.Groups;
import com.vesperin.text.Selection;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    try {
      // check if method file was given
      if(from != null){
        final Path         methods  = Paths.get(from);
        final List<String> allLines = IO.readLines(methods);

        final Set<Source> corpusSet = corpus.stream().collect(Collectors.toSet());

        final Set<String> relevantSet = Sources.populate(corpus, allLines);
        System.out.println(Selection.selects(topK, corpusSet, relevantSet));

      } else if (target != null){
        final Path start = Paths.get(target);
        corpus.addAll(Sources.from(IO.collectFiles(start, "java", "Test", "test", "package-info")));

        final Set<Source> corpusSet = corpus.stream().collect(Collectors.toSet());

        System.out.println("[INFO] Number of files in corpus? " + corpus.size());

        if(topK <= 0) {
          Selection.selects(Integer.MAX_VALUE, corpusSet);
          System.out.println(Selection.selects(corpusSet));
        } else {
          StopWords.GENERAL.add(Paths.get(target).getFileName().toString());
          StopWords.JAVA.addAll(Arrays.asList("scala", "get", "max"));

          final List<Word> words = Selection.selects(
            topK, corpusSet, StopWords.JAVA, StopWords.ENGLISH, StopWords.GENERAL
          );

          final Groups groups = Grouping.formGroups(words);

          System.out.println("========================");
          System.out.println("Clusters of semantically similar words:");
          for(Group each : groups){
            System.out.println(each);
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
