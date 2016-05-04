package com.vesperin.cue.spi;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class GraphUtils {

  /**
   * Color used to mark unvisited nodes
   */
  private static final int VISIT_COLOR_WHITE = 1;

  /**
   * Color used to mark nodes as they are first visited in DFS order
   */
  private static final int VISIT_COLOR_GREY = 2;

  /**
   * Color used to mark nodes after descendants are completely visited
   */
  private static final int VISIT_COLOR_BLACK = 4;

  /**
   * Private constructor
   **/
  private GraphUtils() {
  }


  /**
   * Returns the depth of a vertex in a graph.
   *
   * @param depth               the incrementing depth
   * @param vertex              the vertex to be searched
   * @param nodesAtCurrentDepth the vertices at the current depth
   * @param <T>                 type of data stored in a vertex
   * @return the depth of the searched vertex; -1 if not found.
   */
  public static <T> int depth(int depth, Vertex<T> vertex, List<Vertex<T>> nodesAtCurrentDepth) {
    List<Vertex<T>> nodesAtNextLevel = new ArrayList<Vertex<T>>();
    for (Vertex<T> each : nodesAtCurrentDepth) {
      if (each.equals(vertex)) {
        return depth;
      }

      if (each.getOutgoingEdgeCount() > 0) {
        for (Edge<T> child : each.getOutgoingEdges()) {
          nodesAtNextLevel.add(child.getTo());
        }
      }
    }

    if (!nodesAtNextLevel.isEmpty()) {
      return depth(depth + 1, vertex, nodesAtNextLevel);
    }

    return -1; // nothing was found
  }


  /**
   * Search the graph for cycles. In order to detect cycles, we use a modified
   * depth first search called a colored DFS. All nodes are initially marked
   * white. When a node is encountered, it is marked grey, and when its
   * descendants are completely visited, it is marked black. If a grey node is
   * ever encountered, then there is a cycle.
   *
   * @return the edges that form cycles in the graph. The array will be empty if
   * there are no cycles.
   */
  public static <T> Edge<T>[] findCycles(DirectedGraph<T> graph) {
    ArrayList<Edge<T>> cycleEdges = new ArrayList<Edge<T>>();
    // Mark all vertices as white
    for (int n = 0; n < graph.getVertices().size(); n++) {
      Vertex<T> v = graph.getVertex(n);
      v.setMarkState(VISIT_COLOR_WHITE);
    }

    for (int n = 0; n < graph.getVertices().size(); n++) {
      Vertex<T> v = graph.getVertex(n);
      visit(v, cycleEdges);
    }

    @SuppressWarnings("unchecked") Edge<T>[] cycles = new Edge[cycleEdges.size()];
    cycleEdges.toArray(cycles);
    return cycles;
  }


  private static <T> void visit(Vertex<T> v, ArrayList<Edge<T>> cycleEdges) {
    v.setMarkState(VISIT_COLOR_GREY);
    int count = v.getOutgoingEdgeCount();
    for (int n = 0; n < count; n++) {
      Edge<T> e = v.getOutgoingEdge(n);
      Vertex<T> u = e.getTo();
      if (u.getMarkState() == VISIT_COLOR_GREY) {
        // A cycle Edge<T>
        cycleEdges.add(e);
      } else if (u.getMarkState() == VISIT_COLOR_WHITE) {
        visit(u, cycleEdges);
      }
    }

    v.setMarkState(VISIT_COLOR_BLACK);
  }

  /**
   * Check whether a child vertex is a descendant (immediate or distant) of a
   * parent vertex.
   *
   * @param child  The child vertex
   * @param parent The parent vertex
   * @return true if the child is a descendant of the parent; false otherwise.
   */
  public static <T> boolean isDescendantOf(Vertex<T> child, Vertex<T> parent) {

    final Deque<Vertex<T>> stack = new LinkedList<>();
    if(child != null) stack.push(child);

    final Set<Vertex<T>> visited = Sets.newLinkedHashSet();

    while (!stack.isEmpty()) {
      final Vertex<T> current = stack.pop();
      visited.add(current);

      for (Edge<T> each : current.getIncomingEdges()) {
        if (each.getFrom().equals(parent)) return true;
        if (!visited.contains(each.getFrom())) {
          visited.add(each.getFrom());
        }
      }
    }

    return false;
  }
}
