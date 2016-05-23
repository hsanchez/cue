package com.vesperin.cue.graph;

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
