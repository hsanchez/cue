package com.vesperin.cue.spi;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Huascar Sanchez
 */
public class VertexImpl<T> implements Vertex<T> {
  private final List<Edge<T>> incomingEdges;
  private final List<Edge<T>> outgoingEdges;
  private String name;

  private int markState;
  private T data;

  /**
   * Calls this(null, null).
   */
  public VertexImpl() {
    this(null, null);
  }

  /**
   * Create a vertex with the given name and no data
   *
   * @param n the name of the vertex
   */
  public VertexImpl(String n) {
    this(n, null);
  }

  /**
   * Create a vertex with the given data and no name
   *
   * @param data data associated with vertex.
   */
  public VertexImpl(T data) {
    this(null, data);
  }

  /**
   * Create a Vertex with name n and given data
   *
   * @param n    name of vertex
   * @param data data associated with vertex
   */
  public VertexImpl(String n, T data) {
    this.incomingEdges = new ArrayList<>();
    this.outgoingEdges = new ArrayList<>();
    this.name = n;
    this.data = data;
  }

  /**
   * @return the possibly null name of the vertex
   */
  public String getName() {
    return name;
  }

  /**
   * @return the possibly null data of the vertex
   */
  public T getData() {
    return this.data;
  }

  @Override public boolean equals(Object o) {
    return Vertex.class.isInstance(o) && getData().equals(((Vertex)o).getData());
  }

  @Override public int hashCode() {
    return getData().hashCode();
  }


  @Override public void setData(T data) {
    this.data = data;
  }

  @Override public List<Edge<T>> getIncomingEdges() {
    return incomingEdges;
  }

  @Override public List<Edge<T>> getOutgoingEdges() {
    return outgoingEdges;
  }

  @Override public int getMarkState() {
    return markState;
  }

  @Override public void setMarkState(int state) {
    markState = state;
  }

  @Override public String toString() {
    StringBuilder tmp = new StringBuilder("Vertex(");
    tmp.append(name);
    tmp.append(", data=");
    tmp.append(data);
    tmp.append("), in:[");
    for (int i = 0; i < getIncomingEdgeCount(); i++) {
      Edge<T> e = getIncomingEdge(i);
      if (i > 0)
        tmp.append(',');
      tmp.append('{');
      tmp.append(e.getFrom().getName());
      tmp.append(',');
      tmp.append(e.getCost());
      tmp.append('}');
    }
    tmp.append("], out:[");
    for (int i = 0; i < getOutgoingEdgeCount(); i++) {
      Edge<T> e = getOutgoingEdge(i);
      if (i > 0)
        tmp.append(',');
      tmp.append('{');
      tmp.append(e.getTo().getName());
      tmp.append(',');
      tmp.append(e.getCost());
      tmp.append('}');
    }
    tmp.append(']');
    return tmp.toString();
  }
}
