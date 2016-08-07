package com.vesperin.cue.cmds;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.google.common.base.Joiner;
import com.vesperin.base.Source;
import com.vesperin.cue.BasicCli;
import com.vesperin.cue.utils.IO;
import com.vesperin.cue.utils.Sources;
import com.vesperin.text.Grouping;
import com.vesperin.text.Grouping.Group;
import com.vesperin.text.Grouping.Groups;
import com.vesperin.text.Index;
import com.vesperin.text.Query;
import com.vesperin.text.Query.Result;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.vesperin.text.Selection.Document;
import static com.vesperin.text.Selection.selects;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * @author Huascar Sanchez
 */
@Command(name = "concepts", description = "Recognizing implied concepts in code")
public class ConceptAssignmentCommand implements BasicCli.CliCommand {

  private static final String MAP_SET_TYPE_FILE_NAME = "typestowords.txt";
  private static final String MAP_SET_WORD_FILE_NAME = "wordstotypes.txt";
  private static final String MAP_SET_CLUSTERS_NAME  = "clusters.txt";

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
        System.out.println(selects(topK, corpusSet, relevantSet));

      } else if (target != null){
        final Path start = Paths.get(target);
        corpus.addAll(Sources.from(IO.collectFiles(start, "java", "Test", "test", "package-info")));

        final Set<Source> corpusSet = corpus.stream().collect(Collectors.toSet());

        System.out.println("[INFO] Number of files in corpus? " + corpus.size());

        if(topK <= 0) {
          selects(Integer.MAX_VALUE, corpusSet);
          System.out.println(selects(corpusSet));
        } else {
          StopWords.GENERAL.addAll(
            Arrays.asList(Paths.get(target).getFileName().toString(),
              "released", "initializers",
              "frustum", "automated", "per",
              "stored", "zero", "qualified",
              "cleanup", "normalize", "simple",
              "value", "threading", "destructor",
              "damping", "joystick", "matrixf",
              "aassert", "technique", "iassert",
              "universally", "anim", "mutex"
            )
          );

          StopWords.JAVA.addAll(Arrays.asList("scala", "get", "max", "message", "buffered"));

          final List<Word> words = selects(
            topK, corpusSet, StopWords.JAVA, StopWords.ENGLISH, StopWords.GENERAL
          );


          final Groups  groups = Grouping.formDocGroups(words);
          final Index   index  = groups.index();

          final List<String> clusters = new ArrayList<>();
          final List<String> lines = new ArrayList<>();

          clusters.add("{");
          lines.add("{");

          int c = 1; for(Group eachGroup : groups){
            final List<Document>  query = Group.items(eachGroup, Document.class);

            final Result resultSet = Query.types(query, index);

            clusters.add((query + ": " + resultSet));

            int total = words.size(); for(Document eachDocument : query){
              final Result result = Query.types(Collections.singletonList(eachDocument), index);
              final List<Word> wordList =  Result.items(result, Word.class).stream()
                .sorted((a, b) -> Integer.compare(b.value(), a.value()))
                .collect(Collectors.toList());

              lines.add(eachDocument + ": {" + wordList + "}" + (total > 0 ? "," : ""));
              total--;
            }

            System.out.print("c.");

            c++;
          }
          System.out.println();

          lines.add("}");
          clusters.add("}");

          Path newFile = Paths.get(MAP_SET_CLUSTERS_NAME);
          Files.deleteIfExists(newFile);


          System.out.println("[INFO]: Creating " + MAP_SET_CLUSTERS_NAME + " file.");
          Files.write(
            newFile,
            clusters,
            CREATE, APPEND
          );

          System.out.println("[INFO]: Creating " + MAP_SET_TYPE_FILE_NAME);

          newFile = Paths.get(MAP_SET_TYPE_FILE_NAME);
          Files.deleteIfExists(newFile);

          Files.write(
            newFile,
            lines,
            CREATE, APPEND
          );

          lines.clear();
          clusters.clear();

          System.out.println("[INFO]: Creating "  + MAP_SET_WORD_FILE_NAME + " file.");

          newFile = Paths.get(MAP_SET_WORD_FILE_NAME);
          Files.deleteIfExists(newFile);

          lines.add("{");

          int total = words.size(); for(Word word : words){
            final Result result = Query.methods(Collections.singletonList(word), groups.index());
            final Map<String, String> pairs = new LinkedHashMap<>();

            result.forEach(s -> pairs.put(((Document)s).path(), ((Document)s).method()));

            total--;

            final String entry = word + ":" + "{[" + Joiner.on(",").join(pairs.keySet()) + "], [" + Joiner.on(",").join(pairs.values()) + "]}" + (total > 0 ? "," : "");
            System.out.print("e.");

            lines.add(entry);
          }

          lines.add("}");
          System.out.println();

          Files.write(
            newFile,
            lines,
            CREATE, APPEND
          );

          System.out.println("[INFO]: Files " + MAP_SET_TYPE_FILE_NAME + " and " + MAP_SET_WORD_FILE_NAME + " and " + MAP_SET_CLUSTERS_NAME + " were created.");

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
