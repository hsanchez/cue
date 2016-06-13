package com.vesperin.cue;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.parser.errors.ParseException;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Huascar Sanchez
 */
public class Cue implements IntrospectorWithCli {
  private final Runner runner;

  /**
   * Constructs a new Introspector with a CLI runner.
   * @param runner a new Cli Runner.
   */
  private Cue(Runner runner){
    this.runner = runner;
  }

  /**
   * @return a new Introspector object.
   */
  public static Introspector newIntrospector(){
    return newIntrospector(new Console());
  }

  /**
   * @return a new Introspector object.
   */
  private static Introspector newIntrospector(Runner runner){
    Objects.requireNonNull(runner);
    return new Cue(runner);
  }

  /**
   * @return a new IntrospectorWithCli object.
   */
  private static IntrospectorWithCli newIntrospectorWithCli(){
    return (IntrospectorWithCli) newIntrospector();
  }

  /**
   * Executes the CLI given some arguments.
   *
   * @param args command arguments.
   */
  private static void executeCli(String[] args){
    executeCli(newIntrospectorWithCli(), args);
  }

  /**
   * Executes the CLI of an Introspector object and its string arguments.
   *
   * @param introspector Introspector object.
   * @param args the command arguments.
   */
  private static void executeCli(IntrospectorWithCli introspector, String[] args){
    Objects.requireNonNull(introspector);
    Objects.requireNonNull(args);

    final Cli<CliCommand>  cueCli  = introspector.buildCli();
    try {
      final CliCommand cmd = cueCli.parse(args);
      introspector.run(cmd);
    } catch (ParseException e) {
      System.err.println("Parser error: " + e.getMessage());
    } catch (Throwable e) {
      System.err.println("Unexpected error: " + e.getMessage());
      e.printStackTrace(System.err);
    }
  }


  @Override public Runner getCliRunner() {
    return runner;
  }

  private static class Console implements Runner {
    @Override public Result run(CliCommand command) {
      try {
        final ExecutorService service = Executors.newSingleThreadExecutor();
        final Future<Integer> result  = service.submit(command);
        int exitCode = result.get();
        System.out.println();
        System.out.println("Exiting with Code " + exitCode);
        System.exit(exitCode);
      } catch (Throwable e) {
        System.err.println("Command threw error: " + e.getMessage());
        e.printStackTrace(System.err);
      }

      return null;
    }
  }

  public static void main(String[] args) {
    Cue.executeCli(args);
  }

}
