package com.vesperin.cue;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.builder.CliBuilder;
import com.github.rvesse.airline.parser.errors.ParseException;
import com.vesperin.cue.cmds.CommandRunnable;
import com.vesperin.cue.cmds.ConceptsRecog;
import com.vesperin.cue.cmds.Typicality;

/**
 * @author Huascar Sanchez
 */
public class BasicCli {
  public static void main(String[] args) {
    final CliBuilder<CommandRunnable> builder = Cli.<CommandRunnable>builder("cue")
      .withDescription("Cue CLI")
      .withCommand(Typicality.class)
      .withCommand(ConceptsRecog.class);

    executeCli(builder.build(), args);
  }

  private static <T extends CommandRunnable> void execute(T cmd) {
    try {
      int exitCode = cmd.run();
      System.out.println();
      System.out.println("Exiting with Code " + exitCode);
      System.exit(exitCode);
    } catch (Throwable e) {
      System.err.println("Command threw error: " + e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  private static <T extends CommandRunnable> void executeCli(Cli<T> cli, String[] args) {
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
}
