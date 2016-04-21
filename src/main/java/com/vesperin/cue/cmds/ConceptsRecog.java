package com.vesperin.cue.cmds;

import com.github.rvesse.airline.annotations.Command;

/**
 * @author Huascar Sanchez
 */
@Command(name = "concepts", description = "Recognizing implied concepts in code")
public class ConceptsRecog implements CommandRunnable {
  @Override public int run() {
    return 0;
  }
}
