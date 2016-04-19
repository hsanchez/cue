package com.vesperin.cue.spi;

/**
 * @author Huascar Sanchez
 */
public class EdgeImpl<T> implements Edge <T> {
  private final Vertex<T> from;
  private final Vertex<T> to;
  private int cost;

  /**
   * Create a zero cost edge between from and to
   *
   * @param from
   *          the starting vertex
   * @param to
   *          the ending vertex
   */
  public EdgeImpl(Vertex<T> from, Vertex<T> to) {
    this(from, to, 0);
  }

  /**
   * Create an edge between from and to with the given cost.
   *
   * @param from
   *          the starting vertex
   * @param to
   *          the ending vertex
   * @param cost
   *          the cost of the edge
   */
  public EdgeImpl(Vertex<T> from, Vertex<T> to, int cost) {
    this.from = from;
    this.to = to;
    this.cost = cost;
  }

  /**
   * Get the ending vertex
   *
   * @return ending vertex
   */
  @Override public Vertex<T> getTo() {
    return to;
  }

  /**
   * Get the starting vertex
   *
   * @return starting vertex
   */
  @Override public Vertex<T> getFrom() {
    return from;
  }

  /**
   * Get the cost of the edge
   *
   * @return cost of the edge
   */
  @Override public int getCost() {
    return cost;
  }

  /**
   * String rep of edge
   *
   * @return string rep with from/to vertex names and cost
   */
  public String toString() {
    return "Edge[from: " + from.getName() + ",to: " + to.getName() + ", cost: " + cost + "]";
  }
}
