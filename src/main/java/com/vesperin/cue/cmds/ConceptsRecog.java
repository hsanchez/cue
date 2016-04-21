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
import java.util.List;

/**
 * @author Huascar Sanchez
 */
@Command(name = "concepts", description = "Recognizing implied concepts in code")
public class ConceptsRecog implements CommandRunnable {

  @Inject HelpOption<Typicality> help;


  @Option(name = {"-d", "--directory"}, arity = 1, description = "target directory containing files to test.")
  private String target = null;

  @Override public int run() {
    if(!help.showHelpIfRequested()){
      if(target == null) {
        System.err.println("Please use a valid option (see -help for information).");
        return -1;
      }

      recognizeConcepts(target);
    }

    return 0;
  }

  private static void recognizeConcepts(String target) {
    final Path start = Paths.get(target);
    final List<File> allFiles = IO.collectFiles(start, "java");

    final Cue cue = new Cue();

    for(File eachFile : allFiles){
      final Source src = Sources.from(eachFile);
      System.out.printf("%-32s %s\n", src.getName(), cue.assignedConcepts(src));
    }
  }
}
