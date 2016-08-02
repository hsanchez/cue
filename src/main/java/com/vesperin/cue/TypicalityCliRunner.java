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
public class TypicalityCliRunner implements TypicalityWithCli {
  private final Runner runner;

  /**
   * Constructs a new Typicality with a CLI runner.
   * @param runner a new Cli Runner.
   */
  private TypicalityCliRunner(Runner runner){
    this.runner = runner;
  }

  /**
   * @return a new Typicality object.
   */
  public static TypicalityWithCli newTypicalityCli(){
    return newTypicalityCli(new Console());
  }

  /**
   * @return a new Typicality object.
   */
  private static TypicalityWithCli newTypicalityCli(Runner runner){
    Objects.requireNonNull(runner);
    return new TypicalityCliRunner(runner);
  }

  /**
   * @return a new TypicalityWithCli object.
   */
  private static TypicalityWithCli newIntrospectorWithCli(){
    return newTypicalityCli();
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
   * Executes the CLI of an Typicality object and its string arguments.
   *
   * @param introspector Typicality object.
   * @param args the command arguments.
   */
  private static void executeCli(TypicalityWithCli introspector, String[] args){
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
    @Override public Output run(CliCommand command) {
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
    TypicalityCliRunner.executeCli(args);
  }

}
