package com.vesperin.cue.cmds;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;
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

  @Option(name = {"-d", "--directory"}, arity = 1, description = "target directory containing files to test.")
  private String target = null;

  @Option(name = { "-f", "--files" }, arity = 100, description = "multiple files to test.")
  private List<String> files = null;

  @Option(name = {"-k", "--topK"}, description = "k most typical source code.")
  private int topK = 1;

  @Override public int run() {
    if(!help.showHelpIfRequested()){
      if(files == null && target == null) {
        System.err.println("Please use a valid option (see -help for information).");
        return -1;
      }

      if(files != null && target != null) {
        System.err.println("Please use only one option at a time (not both).");
        return -1;
      }

      final List<Source> corpus = new ArrayList<>();
      if(files != null){

        generateSourceCode(corpus, files);
        performTypicalityQuery(corpus, topK);

      } else {
        catchAndQuery(corpus, target, topK);
      }
    }

    return 0;
  }

  private static void catchAndQuery(List<Source> corpus, String target, int topK) {
    final Path start = Paths.get(target);
    final List<File> allFiles = IO.collectFiles(start, "java");

    for(File each : allFiles){
      final Source src = Sources.from(each);
      corpus.add(src);
    }

    performTypicalityQuery(corpus, topK);
  }

  private static void performTypicalityQuery(List<Source> corpus, int topK) {
    final Cue cue = new Cue();

    final List<Source> result = cue.typicalityQuery(corpus, topK);
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

  private static void generateSourceCode(List<Source> corpus, List<String> files) {
    for (String path : files){
      final Path eachPath = Paths.get(path);
      corpus.add(Sources.from(eachPath.toFile()));
    }
  }
}
