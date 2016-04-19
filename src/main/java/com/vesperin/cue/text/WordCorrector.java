package com.vesperin.cue.text;

import java.util.Objects;
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

  /**
   * Calculates the similarity between two strings.
   * @param word original string
   * @param suggestion suggested string
   * @return similarity score.
   */
  static float similarity(String word, String suggestion){
    return 1.0f - normalizeDistance(word, suggestion);
  }

  /**
   * Calculates the normalized distance of a suggested correction. This is
   * no longer a metric. Therefore, in order to calculate the similarity
   * between two words we must subtract this value from 1 (see
   * {@link #similarity(String, String)} method for details).
   *
   *
   * @param word original word
   * @param suggestion suggested correction for original word
   * @return minimum score to use.
   */
  static float normalizeDistance(String word, String suggestion){
    Objects.requireNonNull(word);
    Objects.requireNonNull(suggestion);


    final float editDistance = (distance(word, suggestion)/1.0f);
    final float length       = Math.max(word.length(),suggestion.length())/1.0f;

    return (editDistance/length);
  }

  /**
   * Edit distance between words
   *
   * @param a original word
   * @param b suggested correction.
   * @return the edit distance.
   */
  static int distance(String a, String b){
    if(a == null || b == null)  return 0;
    if(a.length() == 0)         return 0;
    if(b.length() == 0)         return 0;
    if(a.equals(b))             return 0;


    int[] v0 = new int[b.length() + 1];
    int[] v1 = new int[b.length() + 1];

    int idx;
    for(idx = 0; idx < v0.length; idx++){
      v0[idx] = idx;
    }

    for(idx = 0; idx < a.length(); idx++){
      v1[0] = idx + 1;

      for (int j = 0; j < b.length(); j++){
        int cost = (a.charAt(idx) == b.charAt(j) ? 0 : 1);

        v1[j + 1] = Math.min(Math.min(v1[j] + 1, v0[j + 1] + 1), v0[j] + cost);
      }

      System.arraycopy(v1, 0, v0, 0, v0.length);
    }

    return v1[b.length()];
  }

  static boolean onlyConsonants(String word) {
    // thx to http://stackoverflow.com/q/26536829/26536928
    return !(word == null || word.isEmpty()) && word.matches("[^aeiou]+$");
  }

  static boolean isNumber(String input) {
    // thx to http://stackoverflow.com/q/15111420/15111450
    return !(input == null || input.isEmpty()) && input.matches("\\d+");
  }

  static boolean isStopWord(Set<StopWords> stops, String word){
    return StopWords.isStopWord(stops, word);
  }
}
