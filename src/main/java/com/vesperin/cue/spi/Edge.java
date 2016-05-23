package com.vesperin.cue.spi;

/**
 * @author Huascar Sanchez
 */
public interface Edge <V> {
  /**
   * @return the edge's source vertex
   */
  V from();

  /**
   *
   * @return the edge's destination vertex
   */
  V to();

  /**
   * @return the edge's weight/cost
   */
  double weight();

  @Override String toString();
}
