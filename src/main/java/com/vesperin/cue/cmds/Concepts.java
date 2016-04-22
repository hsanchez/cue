package com.vesperin.cue.cmds;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.vesperin.base.Source;
import com.vesperin.cue.Cue;
import com.vesperin.cue.utils.IO;
import com.vesperin.cue.utils.Sources;

import javax.inject.Inject;
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
public class Concepts implements CommandRunnable {

  @Inject HelpOption<Typicality> help;


  @Option(name = {"-f", "--from"}, arity = 1, description = "extracts concepts from target directory.")
  private String target = null;

  @Option(name = {"-k", "--topk"}, description = "k most typical source code.")
  private int topK = 10;

  @Option(name = {"-m", "--methods"}, arity = 1, description = "relevant method names")
  private String relevant = null;


  @Override public int run() {
    if(!help.showHelpIfRequested()){
      if(target == null) {
        System.err.println("Please use a valid option (see -help for information).");
        return -1;
      }

      return conceptAssignment(target, relevant, topK);
    }

    return 0;
  }

  private static int conceptAssignment(String target, String relevant, int topK) {
    final Path start = Paths.get(target);
    try {
      final List<Source> allFiles = IO.collectFiles(start, "java").stream()
        .map(Sources::from).collect(Collectors.toList());

      final Cue cue = new Cue();

      if(relevant != null){
        final Path filePath = Paths.get(relevant);
        if(Files.exists(filePath)){
          // assumption is that the main method is already in this set
          final Set<String> relevantSet = Files.readAllLines(filePath, Charset.defaultCharset())
            .stream()
            .collect(Collectors.toSet());

          System.out.println(cue.assignedConcepts(allFiles, topK, relevantSet));
        } else {
          System.err.println("Ignore file (" + relevant + ") does not exist!");
          return -1;
        }
      } else {
        // the entire body declaration (e.g., all methods) is relevant
        System.out.println(cue.assignedConcepts(allFiles, topK));
      }

      return 0;
    } catch (Exception e){
      e.printStackTrace(System.err);
      return -1;
    }
  }
}
