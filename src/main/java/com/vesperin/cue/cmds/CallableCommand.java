package com.vesperin.cue.cmds;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * @author Huascar Sanchez
 */
public interface CallableCommand extends Callable <Integer> {
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
   * Runs the command and returns an exit code that
   * the application should return.
   *
   * @return exit code
   */
  @Override Integer call() throws Exception;
}
