package com.vesperin.cue.utils;

import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
public class Similarity {
  private Similarity(){
    throw new Error("Cannot be instantiated");
  }

  /**
   * Calculates the similarityScore between two strings.
   * @param word original string
   * @param suggestion suggested string
   * @return similarityScore score.
   */
  public static float similarityScore(String word, String suggestion){
    return 1.0f - normalizeDistance(word, suggestion);
  }

  /**
   * Calculates the normalized distance of a suggested correction. This is
   * no longer a metric. Therefore, in order to calculate the similarityScore
   * between two words we must subtract this value from 1 (see
   * {@link #similarityScore(String, String)} method for details).
   *
   *
   * @param word original word
   * @param suggestion suggested correction for original word
   * @return minimum score to use.
   */
  public static float normalizeDistance(String word, String suggestion){
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
  public static int distance(String a, String b){
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
}
