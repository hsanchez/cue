package com.vesperin.cue.spi;

/**
 * A directed, weighted edge in a graph
 *
 * @param <T>
 */
public interface Edge<T> {

  /**
   * Get the ending vertex
   *
   * @return ending vertex
   */
  Vertex<T> getTo();

  /**
   * Get the starting vertex
   *
   * @return starting vertex
   */
  Vertex<T> getFrom();

  /**
   * Get the cost of the edge
   *
   * @return cost of the edge
   */
  int getCost();

  /**
   * String rep of edge
   *
   * @return string rep with from/to vertex names and cost
   */
  @Override String toString();
}

