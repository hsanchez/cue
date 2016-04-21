package com.vesperin.cue.cmds;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.vesperin.base.Source;
import com.vesperin.cue.Cue;
import com.vesperin.cue.utils.IO;
import com.vesperin.cue.utils.Sources;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
@Command(name = "concepts", description = "Recognizing implied concepts in code")
public class ConceptsRecog implements CommandRunnable {

  @Inject HelpOption<Typicality> help;


  @Option(name = {"-d", "--directory"}, arity = 1, description = "target directory containing files to test.")
  private String target = null;

  @Option(name = {"-k", "--topK"}, description = "k most typical source code.")
  private int topK = 10;

  @Override public int run() {
    if(!help.showHelpIfRequested()){
      if(target == null) {
        System.err.println("Please use a valid option (see -help for information).");
        return -1;
      }

      return recognizeConcepts(target, topK);
    }

    return 0;
  }

  private static int recognizeConcepts(String target, int topK) {
    final Path start = Paths.get(target);
    try {
      final List<Source> allFiles = IO.collectFiles(start, "java").stream()
        .map(Sources::from).collect(Collectors.toList());;

      final Cue cue = new Cue();
      final List<String> concepts = cue.assignedConcepts(allFiles, topK);

      System.out.println(concepts);
      return 0;
    } catch (Exception e){
      e.printStackTrace(System.err);
      return -1;
    }
  }
}
