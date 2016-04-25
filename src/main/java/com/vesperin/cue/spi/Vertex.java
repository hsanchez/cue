package com.vesperin.cue.spi;

import java.util.List;

/**
 * A named graph vertex with optional data.
 *
 * @param <T>
 */
public interface Vertex<T> {

  /**
   * @return the possibly null name of the vertex
   */
  String getName();

  /**
   * @return the possibly null data of the vertex
   */
  T getData();

  /**
   * @param data The data to set.
   */
  void setData(T data);

  /**
   * Add an edge to the vertex. If edge.from is this vertex, its an outgoing
   * edge. If edge.to is this vertex, its an incoming edge. If neither from or
   * to is this vertex, the edge is not added.
   *
   * @param e -
   *          the edge to add
   * @return true if the edge was added, false otherwise
   */
  default boolean addEdge(Edge<T> e) {

    if (!hasEdge(e)) {
      if (e.getFrom() == this)
        getOutgoingEdges().add(e);
      else if (e.getTo() == this)
        getIncomingEdges().add(e);
      else
        return false;
      return true;
    }

    return false;
  }

  /**
   * Add an outgoing edge ending at to.
   *
   * @param to   -
   *             the destination vertex
   * @param cost the edge cost
   */
  default void addOutgoingEdge(Vertex<T> to, int cost) {
    Edge<T> outgoing = new EdgeImpl<>(this, to, cost);
    addEdge(outgoing);
  }

  /**
   * Add an incoming edge starting at from
   *
   * @param from -
   *             the starting vertex
   * @param cost the edge cost
   */
  default void addIncomingEdge(Vertex<T> from, int cost) {
    Edge<T> incoming = new EdgeImpl<>(from, this, cost);
    addEdge(incoming);
  }


  /**
   * Check the vertex for either an incoming or outgoing edge matching e.
   *
   * @param e the edge to check
   * @return true it has an edge
   */
  default boolean hasEdge(Edge<T> e) {
    if (e.getFrom() == this)
      return getIncomingEdges().contains(e);
    else
      return e.getTo() == this && getOutgoingEdges().contains(e);
  }

  /**
   * Remove an edge from this vertex
   *
   * @param e -
   *          the edge to remove
   * @return true if the edge was removed, false if the edge was not connected
   * to this vertex
   */
  default boolean remove(Edge<T> e) {
    if (hasEdge(e)) {
      if (e.getFrom() == this)
        getIncomingEdges().remove(e);
      else if (e.getTo() == this)
        getOutgoingEdges().remove(e);
      else
        return false;
      return true;
    }

    return false;
  }

  /**
   * @return the count of incoming edges
   */
  default int getIncomingEdgeCount() {
    return getIncomingEdges().size();
  }

  /**
   * Get the ith incoming edge
   *
   * @param i the index into incoming edges
   * @return ith incoming edge
   */
  default Edge<T> getIncomingEdge(int i) {
    return getIncomingEdges().get(i);
  }

  /**
   * Get the incoming edges
   *
   * @return incoming edge list
   */
  List<Edge<T>> getIncomingEdges();

  /**
   * @return the count of incoming edges
   */
  default int getOutgoingEdgeCount() {
    return getOutgoingEdges().size();
  }

  /**
   * Get the ith outgoing edge
   *
   * @param i the index into outgoing edges
   * @return ith outgoing edge
   */
  default Edge<T> getOutgoingEdge(int i) {
    return getOutgoingEdges().get(i);
  }

  /**
   * Get the outgoing edges
   *
   * @return outgoing edge list
   */
  List<Edge<T>> getOutgoingEdges();

  /**
   * Search the outgoing edges looking for an edge whose edge.to == dest.
   *
   * @param dest the destination
   * @return the outgoing edge going to dest if one exists, null otherwise.
   */
  default Edge<T> findEdge(Vertex<T> dest) {
    for (Edge<T> e : getOutgoingEdges()) {
      if (e.getTo().equals(dest))
        return e;
    }
    return null;
  }

  /**
   * Search the outgoing edges for a match to e.
   *
   * @param e -
   *          the edge to check
   * @return e if its a member of the outgoing edges, null otherwise.
   */
  default Edge<T> findEdge(Edge<T> e) {
    if (getOutgoingEdges().contains(e))
      return e;
    else
      return null;
  }

  /**
   * What is the cost from this vertex to the dest vertex.
   *
   * @param dest the destination vertex.
   * @return Return Integer.MAX_VALUE if we have no edge to dest, 0 if dest is
   * this vertex, the cost of the outgoing edge otherwise.
   */
  default int cost(Vertex<T> dest) {
    if (dest == this)
      return 0;

    Edge<T> e = findEdge(dest);
    int cost = Integer.MAX_VALUE;
    if (e != null)
      cost = e.getCost();
    return cost;
  }

  /**
   * Is there an outgoing edge ending at dest.
   *
   * @param dest the vertex to check
   * @return true if there is an outgoing edge ending at vertex, false
   * otherwise.
   */
  default boolean hasEdge(Vertex<T> dest) {
    return (findEdge(dest) != null);
  }

  /**
   * Get the mark state value.
   *
   * @return the mark state
   */
  int getMarkState();

  /**
   * Set the mark state to state.
   *
   * @param state the state
   */
  void setMarkState(int state);
}