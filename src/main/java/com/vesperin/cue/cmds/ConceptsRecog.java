package com.vesperin.cue.cmds;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
//import com.google.common.io.Files;
import com.vesperin.base.Source;
import com.vesperin.cue.Cue;
import com.vesperin.cue.utils.IO;
import com.vesperin.cue.utils.Sources;

import javax.inject.Inject;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
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

  @Option(name = {"-i", "--ignore"}, arity = 1, description = "file containing a list of method to exclude during processing.")
  private String ignore = null;


  @Override public int run() {
    if(!help.showHelpIfRequested()){
      if(target == null) {
        System.err.println("Please use a valid option (see -help for information).");
        return -1;
      }

      return recognizeConcepts(target, ignore, topK);
    }

    return 0;
  }

  private static int recognizeConcepts(String target, String ignore, int topK) {
    final Path start = Paths.get(target);
    try {
      final List<Source> allFiles = IO.collectFiles(start, "java").stream()
        .map(Sources::from).collect(Collectors.toList());

      final Cue cue = new Cue();

      if(ignore != null){
        final Path filePath = Paths.get(ignore);
        if(Files.exists(filePath)){
          // assumption is that the main method is already in this set
          final Set<String> ignoredSet = Files.readAllLines(filePath, Charset.defaultCharset()).stream().collect(Collectors.toSet());
          System.out.println(cue.assignedConcepts(allFiles, topK, ignoredSet));
        } else {
          System.err.println("Ignore file (" + ignore + ") does not exist!");
          return -1;
        }
      } else {
        System.out.println(cue.assignedConcepts(allFiles, topK));
      }

      return 0;
    } catch (Exception e){
      e.printStackTrace(System.err);
      return -1;
    }
  }
}
