package com.vesperin.cue.cmds;

/**
 * @author Huascar Sanchez
 */
public interface CommandRunnable {
  /**
   * Runs the command and returns an exit code that
   * the application should return.
   *
   * @return exit code
   */
  int run();
}
