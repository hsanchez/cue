package com.vesperin.cue;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.builder.CliBuilder;
import com.vesperin.cue.cmds.CallableCommand;
import com.vesperin.cue.cmds.ConceptAssignmentCommand;
import com.vesperin.cue.cmds.RepresentativeAnalysisCommand;
import com.vesperin.cue.cmds.TypicalityAnalysisCommand;

import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
interface IntrospectorWithCli extends Introspector {

  /**
   * @return a new CLI runner.
   */
  Runner getCliRunner();

  /**
   * Runs a {@link CallableCommand CLI command}.
   *
   * @param command CLI command
   * @return Runner's result.
   */
  default Result run(CallableCommand command) {
    return run(command, getCliRunner());
  }

  /**
   * Runs the configured CLI.
   *
   * @param withRunner CLI's runner strategy.
   * @return Runner's result.
   */
  default Result run(CallableCommand command, Runner withRunner){
    return withRunner.run(command);
  }

  /**
   * Builds its own 'default' CLI.
   *
   * @return the new Introspector's CLI
   */
  default Cli<CallableCommand> buildCli(){
    return buildCli(Cli.<CallableCommand>builder("cue")
      .withDescription("Cue CLI")
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
  default Cli<CallableCommand> buildCli(CliBuilder<CallableCommand> builder){
    return Objects.requireNonNull(builder).build();
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
    Result run(CallableCommand command);
  }

  /**
   * Runner's result object.
   */
  interface Result {
    @Override String toString();
  }
}
