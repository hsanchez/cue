package com.vesperin.cue.cmds;

import java.util.concurrent.Callable;

/**
 * @author Huascar Sanchez
 */
public interface CallableCommand extends Callable <Integer> {
  /**
   * Runs the command and returns an exit code that
   * the application should return.
   *
   * @return exit code
   */
  @Override Integer call() throws Exception;
}
