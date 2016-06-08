package com.vesperin.cue.cmds;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.google.common.base.Strings;
import com.vesperin.base.Source;
import com.vesperin.cue.Cue;
import com.vesperin.cue.Introspector;
import com.vesperin.cue.utils.IO;
import com.vesperin.cue.utils.Sources;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.vesperin.cue.cmds.CallableCommand.allNonNull;
import static com.vesperin.cue.cmds.CallableCommand.allNull;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * @author Huascar Sanchez
 */
@Command(name = "typical", description = "Finds the top k most typical implementation")
public class TypicalityAnalysisCommand implements CallableCommand {

  private static final String TYPICAL_SET_FILE_NAME = "typicalset.txt";

  @Inject HelpOption<TypicalityAnalysisCommand> help;

  @Option(name = { "-t", "--targets" }, arity = 100, description = "the names of multiple files to analyze (100 max).")
  private List<String> targets = null;

  @Option(name = {"-f", "--from"}, arity = 1, description = "analyze only the method entries in target file.")
  private String from = null;

  @Option(name = {"-d", "--directory"}, arity = 1, description = "directory containing files to analyze.")
  private String directory = null;

  @Option(name = {"-k", "--topK"}, description = "k most typical source code.")
  private int topK = 1;

  @SuppressWarnings("FieldCanBeLocal")
  @Option(name = {"-b", "--bandwidth"}, description = "bandwidth parameter.")
  private double bandwidth = 0.3;

  @SuppressWarnings("FieldCanBeLocal")
  @Option(name = {"-e", "--echo"}, description = "print results on screen.")
  private boolean onScreen = false;

  @Override public Integer call() throws Exception {
    if(!help.showHelpIfRequested()){
      if(allNull(3, directory, from, targets)) {
        System.err.println("Please use a valid option (see -help for information).");
        return -1;
      }

      if(topK < 1) {
        System.err.println("Please use a valid topK value (see -help for information).");
        return -1;
      }

      if(allNonNull(3, directory, from, targets)) {
        System.err.println("Please use only one option at a time (not all).");
        return -1;
      }

      final List<Source> corpus = new ArrayList<>();
      if(targets != null){

        final Set<String> relevant = Sources.populate(corpus, targets);
        performTypicalityQuery(corpus, relevant);

      } else {
        if(from != null){
          catchAndQuery(corpus);
        } else {
          if(directory == null) {
            System.err.println("missing -d or --directory option");
            return -1;
          } else {
            catchDirAndQuery(corpus, directory);
          }
        }
      }
    }

    return 0;
  }

  private void catchDirAndQuery(List<Source> corpus, String target) {
    final Path start = Paths.get(target);
    final List<File> allFiles = IO.collectFiles(start, "java", "test", "Test");
    for(File each : allFiles){
      final Source src = Sources.from(each);
      corpus.add(src);
    }

    performTypicalityQuery(corpus, new HashSet<>());
  }

  private void catchAndQuery(List<Source> corpus) {
    final Path start = Paths.get(from);
    final List<String> allLines = IO.readLines(start);

    final Set<String> relevant = Sources.populate(corpus, allLines);
    performTypicalityQuery(corpus, relevant);
  }

  private void performTypicalityQuery(List<Source> corpus, Set<String> relevant) {

    final Introspector cue = Cue.newIntrospector();
    final Set<Source> corpusSet = corpus.stream().collect(Collectors.toSet());
    final List<Source> result = cue.issueTypicalityQuery(topK, bandwidth, corpusSet, relevant);
    if(result.isEmpty()){
      System.out.println("No typical source code was found.");
    } else {

      try {
        final Path newFile = Paths.get(TYPICAL_SET_FILE_NAME);
        Files.deleteIfExists(newFile);

        for(Source each : result){

          final String snippet = Introspector.relevantSegments(each, relevant);
          if(!Strings.isNullOrEmpty(snippet)){

            if(!onScreen) {
              final List<String> lines = new ArrayList<>();
              lines.add(each.getName() + ":");
              lines.addAll(Sources.contentToLines(snippet));

              Files.write(
                newFile,
                lines,
                CREATE, APPEND
              );
            } else {
              Files.deleteIfExists(newFile);
              System.out.println(each.getName());
              System.out.println(snippet);
              System.out.println();
            }

          }

        }

        if(!onScreen){
          System.out.printf("%-32s\n", newFile.toFile().getAbsolutePath());
        }

      } catch (IOException e){
        throw new RuntimeException(e);
      }
    }
  }
}
