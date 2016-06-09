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
public interface IntrospectorWithCli extends Introspector {
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
}
