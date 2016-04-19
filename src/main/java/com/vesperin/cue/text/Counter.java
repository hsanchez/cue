package com.vesperin.cue.text;

/**
 * @author Huascar Sanchez
 */
public class Counter implements Comparable<Counter> {

  private final String name;     // counter name
  private final int maxCount;    // maximum value
  private int count;             // current value

  /**
   * Creates a new counter with the given parameters
   * @param id the word of interest
   */
  public Counter(String id) {
    this(id, Integer.MAX_VALUE);
  }

  /**
   * Creates a new counter with the given parameters
   *
   * @param id the word of interest
   * @param max max value
   */
  public Counter(String id, int max) {
    name = id;
    maxCount = max;
    count = 0;
  }

  // compare two Counter objects based on their count
  @Override public int compareTo(Counter that) {
    if      (this.count < that.count) return -1;
    else if (this.count > that.count) return +1;
    else                              return  0;
  }

  /**
   * Increment the counter by 1
   */
  public void increment() {
    increment(1);
  }

  /**
   * Increment the counter by 1
   */
  public void increment(int step) {
    if ((count + step) < maxCount) count = count + step;
    assert count >= 1;
  }

  /**
   * @return the current count
   */
  public int value() {
    return count;
  }


  @Override public String toString() {
    return name + ": " + count;
  }


}