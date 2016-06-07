package com.vesperin.cue;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.builder.CliBuilder;
import com.github.rvesse.airline.parser.errors.ParseException;
import com.vesperin.base.Source;
import com.vesperin.cue.cmds.CallableCommand;
import com.vesperin.cue.cmds.ConceptAssignmentCommand;
import com.vesperin.cue.cmds.RepresentativeAnalysisCommand;
import com.vesperin.cue.cmds.TypicalityAnalysisCommand;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Huascar Sanchez
 */
public class Cue {
  private Cue(){
    throw new Error("Utility class");
  }

  /**
   * See {@link Introspector#assignedConcepts(Source)}
   * for further details.
   */
  public static List<String> assignedConcepts(Source code){
    return newIntrospector().assignedConcepts(code);
  }

  /**
   * See {@link Introspector#assignedConcepts(List)}
   * for further details.
   */
  public static List<String> assignedConcepts(List<Source> sources){
    return newIntrospector().assignedConcepts(sources);
  }

  /**
   * See {@link Introspector#assignedConcepts(int, List)}
   * for further details.
   */
  public static List<String> assignedConcepts(int topk, List<Source> sources){
    return newIntrospector().assignedConcepts(topk, sources);
  }

  /**
   * See {@link Introspector#assignedConcepts(int, List, Set)}
   * for further details.
   */
  public static List<String> assignedConcepts(int topK, List<Source> sources, Set<String> relevantSet){
    return newIntrospector().assignedConcepts(topK, sources, relevantSet);
  }

  /**
   * See {@link Introspector#assignedConcepts(Source, Set)}
   * for further details.
   */
  public static List<String> assignedConcepts(Source code, final Set<String> relevant){
    return newIntrospector().assignedConcepts(code, relevant);
  }

  /**
   * See {@link Introspector#issueTypicalityQuery(int, Set, Set)}
   * for further details.
   */
  public static List<Source> issueTypicalityQuery(int topK, Set<Source> resultSet, Set<String> relevant){
    return newIntrospector().issueTypicalityQuery(topK, resultSet, relevant);
  }

  /**
   * See {@link Introspector#issueTypicalityQuery(int, double, Set, Set)}
   * for further details.
   */
  public static List<Source> issueTypicalityQuery(int topK, double h, Set<Source> resultSet, Set<String> relevant){
    return newIntrospector().issueTypicalityQuery(topK, h, resultSet, relevant);
  }

  /**
   * See {@link Introspector#issueRepresentativeQuery(Set, Set)} for further details.
   */
  public static List<Source> issueRepresentativeQuery(Set<Source> resultSet, Set<String> domain){
    return newIntrospector().issueRepresentativeQuery(resultSet, domain);
  }


  /**
   *
   * @return a new Introspector object.
   */
  public static Introspector newIntrospector(){
    return new Introspector(){};
  }

  /**
   * Pulls the code of some method of interest.
   *
   * @param code source file
   * @param relevant relevant method names
   * @return the method snippet.
   */
  public static String relevantCode(Source code, Set<String> relevant){
    return Introspector.relevantSegments(code, relevant);
  }

  private static <T extends CallableCommand> void execute(T cmd) {
    try {
      final ExecutorService service = Executors.newSingleThreadExecutor();
      final Future<Integer> result  = service.submit(cmd);
      int exitCode = result.get();
      System.out.println();
      System.out.println("Exiting with Code " + exitCode);
      System.exit(exitCode);
    } catch (Throwable e) {
      System.err.println("Command threw error: " + e.getMessage());
      e.printStackTrace(System.err);
    }
  }


  private static <T extends CallableCommand> void executeCli(Cli<T> cli, String[] args) {
    try {
      T cmd = cli.parse(args);
      execute(cmd);
    } catch (ParseException e) {
      System.err.println("Parser error: " + e.getMessage());
    } catch (Throwable e) {
      System.err.println("Unexpected error: " + e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  public static void main(String[] args) {
    final CliBuilder<CallableCommand> builder = Cli.<CallableCommand>builder("cue")
      .withDescription("Cue CLI")
      .withCommand(TypicalityAnalysisCommand.class)
      .withCommand(ConceptAssignmentCommand.class)
      .withCommand(RepresentativeAnalysisCommand.class);

    executeCli(builder.build(), args);
  }

}
