package com.vesperin.cue.spi;

/**
 * @author Huascar Sanchez
 */
public interface Vertex <T> {
  /**
   * @return the label given to this vertex.
   */
  String label();

  /**
   * @return the data stored in this vertex.
   */
  T data();

  /**
   * @return the segment's weight/cost
   */
  double weight();

  /**
   * @return the mark state value
   */
  int markState();

  /**
   * Set the mark state to state.
   *
   * @param state new state
   */
  void setMarkState(int state);
}
