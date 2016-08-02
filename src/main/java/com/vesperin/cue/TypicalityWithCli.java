package com.vesperin.cue;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.builder.CliBuilder;
import com.vesperin.cue.cmds.ConceptAssignmentCommand;
import com.vesperin.cue.cmds.RepresentativeAnalysisCommand;
import com.vesperin.cue.cmds.TypicalityAnalysisCommand;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * @author Huascar Sanchez
 */
public interface TypicalityWithCli extends Typicality {

  /**
   * Checks if all the expected number of arguments are non null.
   *
   * @param args args to test.
   * @return true if all args are non null; false otherwise.
   */
  static boolean allNonNull(int expected, Object... args){
    if(Objects.isNull(args))    return false;
    if(expected < 1)            return false;
    if(expected > args.length)  return false;

    for(int idx = 0; idx < expected; idx++){
      if(Objects.isNull(args[idx])) return false;
    }

    return true;
  }

  /**
   * Checks if all the expected number of arguments are null.
   *
   * @param args args to test.
   * @return true if all args are null; false otherwise.
   */
  static boolean allNull(int expected, Object... args){

    if(Objects.isNull(args))    return true;
    if(expected < 1)            return true;
    if(expected > args.length)  return true;


    for(int idx = 0; idx < expected; idx++){
      if(!Objects.isNull(args[idx])) return false;
    }

    return true;
  }

  /**
   * @return a new CLI runner.
   */
  Runner getCliRunner();

  /**
   * Runs a {@link CliCommand CLI command}.
   *
   * @param command CLI command
   * @return Runner's result.
   */
  default Output run(CliCommand command) {
    return run(command, getCliRunner());
  }

  /**
   * Runs the configured CLI.
   *
   * @param withRunner CLI's runner strategy.
   * @return Runner's result.
   */
  default Output run(CliCommand command, Runner withRunner){
    Objects.requireNonNull(withRunner);
    Objects.requireNonNull(command);
    return withRunner.run(command);
  }

  /**
   * Builds its own 'default' CLI.
   *
   * @return the new Typicality's CLI
   */
  default Cli<CliCommand> buildCli(){
    return buildCli(Cli.<CliCommand>builder("cue")
      .withDescription("TypicalityCliRunner CLI")
      .withCommand(TypicalityAnalysisCommand.class)
      .withCommand(ConceptAssignmentCommand.class)
      .withCommand(RepresentativeAnalysisCommand.class)
    );
  }

  /**
   * Builds its own CLI.
   *
   * @param builder non-configured CLI builder.
   * @return a configured CLI builder.
   */
  default Cli<CliCommand> buildCli(CliBuilder<CliCommand> builder){
    return Objects.requireNonNull(builder).build();
  }

  /**
   * CLI command.
   */
  interface CliCommand extends Callable<Integer> {
    /**
     * Runs the command and returns an exit code that
     * the application should return.
     *
     * @return exit code
     */
    @Override Integer call() throws Exception;
  }

  /**
   * CLI's Runner
   */
  interface Runner {
    /**
     * Runs the configured CLI.
     *
     * @param command CLI command.
     * @return output
     */
    Output run(CliCommand command);
  }

  /**
   * Runner's result object.
   */
  interface Output {
    @Override String toString();
  }
}
