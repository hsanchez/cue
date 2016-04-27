package com.vesperin.cue.cmds;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.vesperin.base.Source;
import com.vesperin.cue.Cue;
import com.vesperin.cue.utils.IO;
import com.vesperin.cue.utils.Sources;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Huascar Sanchez
 */
@Command(name = "typical", description = "Finds the top k most typical implementation")
public class Typicality implements CommandRunnable {

  @Inject HelpOption<Typicality> help;

  @Option(name = { "-t", "--targets" }, arity = 100, description = "multiple target files to test (100 max).")
  private List<String> targets = null;

  @Option(name = {"-f", "--from"}, arity = 1, description = "focus in entries in target file.")
  private String from = null;

  @Option(name = {"-d", "--directory"}, arity = 1, description = "directory containing files to check.")
  private String directory = null;

  @Option(name = {"-k", "--topK"}, description = "k most typical source code.")
  private int topK = 1;

  @Option(name = {"-b", "--bandwidth"}, description = "bandwidth parameter.")
  private double bandwidth = 0.3;

  @Override public int run() {
    if(!help.showHelpIfRequested()){
      if(emptyEntries(directory, from, targets)) {
        System.err.println("Please use a valid option (see -help for information).");
        return -1;
      }

      if(containsAll(directory, from, targets)) {
        System.err.println("Please use only one option at a time (not both).");
        return -1;
      }

      final List<Source> corpus = new ArrayList<>();
      if(targets != null){

        Sources.populate(corpus, targets);
        performTypicalityQuery(corpus, bandwidth, topK);

      } else {
        if(from != null){
          catchAndQuery(corpus, from, bandwidth, topK);
        } else {
          catchDirAndQuery(corpus, directory, bandwidth, topK);
        }
      }
    }

    return 0;
  }

  private static void catchDirAndQuery(List<Source> corpus, String target, double h, int topK) {
    final Path start = Paths.get(target);
    final List<File> allFiles = IO.collectFiles(start, "java", "test", "Test");
    for(File each : allFiles){
      final Source src = Sources.from(each);
      corpus.add(src);
    }

    performTypicalityQuery(corpus, h, topK);
  }

  private static void catchAndQuery(List<Source> corpus, String methodsFile, double h, int topK) {
    final Path start = Paths.get(methodsFile);
    final List<String> allLines = IO.readLines(start);

    Sources.populate(corpus, allLines);
    performTypicalityQuery(corpus, h, topK);
  }

  private static void performTypicalityQuery(List<Source> corpus, double h, int topK) {
    final Cue cue = new Cue();

    final List<Source> result = cue.typicalityQuery(corpus, h, topK);
    if(result.isEmpty()){
      System.out.println("No typical source code was found.");
    } else {
      for(Source each : result){

        final String sourceName = each.getName();

        final String className = sourceName.contains(File.separator)
          ? sourceName.substring(sourceName.lastIndexOf(File.separator), sourceName.length())
          : "....";

        System.out.printf("%-32s %s\n", each.getName(), ("public class " + className));
      }
    }
  }

  private static boolean containsAll(String directory, String from, List<String> targets){
    return directory != null && from != null && targets != null;
  }

  private static boolean emptyEntries(String directory, String from, List<String> targets){
    return directory == null && from == null && targets == null;
  }
}
