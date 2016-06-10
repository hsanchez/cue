package com.vesperin.cue.cmds;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.google.common.collect.ImmutableSet;
import com.vesperin.base.Source;
import com.vesperin.cue.Cue;
import com.vesperin.cue.Introspector;
import com.vesperin.cue.utils.IO;
import com.vesperin.cue.utils.Sources;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vesperin.cue.cmds.CallableCommand.allNonNull;
import static com.vesperin.cue.cmds.CallableCommand.allNull;

/**
 * @author Huascar Sanchez
 */
@Command(name = "represent", description = "Find most representative object in set")
public class RepresentativeAnalysisCommand implements CallableCommand {

  @Inject
  HelpOption<TypicalityAnalysisCommand> help;

  @Option(name = { "-t", "--targets" }, arity = 100, description = "the names of multiple files to analyze (100 max).")
  private List<String> targets = null;

  @Option(name = {"-f", "--from"}, arity = 1, description = "focus on entries in target file.")
  private String from = null;

  @Option(name = {"-d", "--directory"}, arity = 1, description = "directory containing files to check.")
  private String directory = null;

  @Override public Integer call() throws Exception {

    if(!help.showHelpIfRequested()){

      if(allNull(3, directory, from, targets)) {
        System.err.println("Please use a valid option (see -help for information).");
        return -1;
      }

      if(allNonNull(3, directory, from, targets)) {
        System.err.println("Please use only one option at a time (not all).");
        return -1;
      }

      final List<Source>  corpus    = new ArrayList<>();


      if(targets != null){
        final Set<String> relevant  = Sources.populate(corpus, targets);
        mostRepresentative(relevant, corpus);
      } else {
        if(from != null){
          final Path start = Paths.get(from);
          final List<String> allLines = IO.readLines(start);

          final Set<String> relevant = Sources.populate(corpus, allLines);
          mostRepresentative(relevant, corpus);
        } else {
          if(directory == null) {
            System.err.println("missing -d or --directory option");
            return -1;
          } else {
            final Path start = Paths.get(directory);
            final List<File> allFiles = IO.collectFiles(start, "java", "test", "Test");
            for(File each : allFiles){
              final Source src = Sources.from(each);
              corpus.add(src);
            }

            mostRepresentative(ImmutableSet.of(), corpus);
          }
        }
      }

    }

    return 0;
  }

  private void mostRepresentative(Set<String> relevant, List<Source> corpus) {
    final Introspector    cue       = Cue.newIntrospector();
    final Set<Source>     corpusSet = corpus.stream().collect(Collectors.toSet());
    final Stream<Source>  stream    = cue.representativeTypicalityQuery(corpusSet, relevant).stream();

    final Optional<Source> optional = stream.findFirst();
    if(optional.isPresent()){
      final Source representative = optional.get();
      final String snippet = Introspector.methodCode(representative, relevant);
      System.out.println(representative.getName());
      System.out.println(snippet);
      System.out.println();
    }
  }
}
