package com.vesperin.cue.text;

import java.util.Locale;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public interface WordCorrector {

  /**
   * Provides a list of corrections for a word. This list will contain
   * items witch a word accuracy of at least 0.5f, per
   * default values.
   *
   * @param word word to be corrected.
   * @return a list of suggested word corrections.
   */
  default String correct(String word){
    return correct(word, 0.5f);
  }

  /**
   * Provides a list of corrections for a word.
   *
   * @param word word to be corrected.
   * @param accuracy minimum score to use.
   * @return a list of suggested word corrections.
   */
  String correct(String word, float accuracy);

  static boolean onlyConsonants(String word) {
    // thx to http://stackoverflow.com/q/26536829/26536928
    return !(word == null || word.isEmpty())
      && word.toLowerCase(Locale.ENGLISH).matches("[^aeiou]+$");
  }

  static boolean isNumber(String input) {
    // thx to http://stackoverflow.com/q/15111420/15111450
    return !(input == null || input.isEmpty()) && input.matches("\\d+");
  }

  static boolean isStopWord(Set<StopWords> stops, String word){
    return StopWords.isStopWord(stops, word);
  }
}
