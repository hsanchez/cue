package com.vesperin.cue.bsg;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class BlockSegmentationGraph extends AbstractSegmentationGraph implements SegmentationGraph {

  /**
   * Creates a new BSG object.
   */
  public BlockSegmentationGraph(){
    super();
  }

  @Override public List<Location> blacklist(int capacity) {
    return generateBlackList(this, capacity);
  }

  private static List<Location> generateBlackList(BlockSegmentationGraph graph, int capacity) {
    final List<Segment> allSegments = Lists.newLinkedList(
      graph.getVertices().stream()
        .map(v -> (Segment) v)
        .collect(Collectors.toList())
    );

    final int N = allSegments.size();

    // if single Block node, then return empty list
    if(N == 1) return Lists.newArrayList();

    @SuppressWarnings("UnnecessaryLocalVariable")
    final int W = capacity;

    double[] profit = new double[N + 1];
    double[] weight = new double[N + 1];

    // add vertices values
    for( int n = 1; n <= N; n++){
      profit[n] = ((Segment)graph.getVertex(n - 1)).getBenefit();
      weight[n] = ((Segment)graph.getVertex(n - 1)).getWeight();
    }

    double[][]  opt = new double [N + 1][W + 1];
    boolean[][] sol = new boolean[N + 1][W + 1];

    for(int n = 1; n <= N; n++){
      for(int w = 1; w <= W; w++){
        // don't take item n
        double option1 = opt[n-1][w];

        // take item n
        double option2 = Double.NEGATIVE_INFINITY;
        if (weight[n] <= w) {
          int weightReduction = (int)(w - weight[n]);
          option2 = profit[n] + opt[n-1][weightReduction];
        }

        // select better of two options only if there is a precedence relation
        // between item n and n - 1
        opt[n][w] = Math.max(option1, option2);
        sol[n][w] = (option2 > option1) && isPrecedenceConstraintMaintained(opt, n, w, graph);
      }
    }

    // determine which items to take
    boolean[] take = new boolean[N+1];
    for (int n = N, w = W; n > 0; n--) {
      if (sol[n][w]) { take[n] = true;  w = (int)(w - weight[n]); }
      else           { take[n] = false;                    }
    }

    final Set<Segment> keep = Sets.newLinkedHashSet();
    for (int n = 1; n <= N; n++) {
      if (take[n]) {
        keep.add(((Segment)graph.getVertex(n - 1)));
      }
    }

    allSegments.removeAll(keep);

    final List<Location> locations = Lists.newLinkedList();
    locations.addAll(
      allSegments.stream()
        .map(foldable -> Locations.locate(foldable.getData()))
        .collect(Collectors.toList())
    );

    return locations;
  }


  private static boolean isPrecedenceConstraintMaintained(
    double[][] opt, int i, int j,
    BlockSegmentationGraph graph) {


    final Segment parent = (Segment) graph.getVertex(i - 1);
    final Segment child = (graph.size() == i
      ? null
      : (Segment) graph.getVertex(i)
    );

    // a graph made of a single node implies the following:
    // - the single node is the root
    // - no precedence constraints can be enforced since it has not parent and no children
    final boolean singleNode    = graph.size() == 1;
    final boolean pass          = singleNode && graph.isRootVertex(parent) && child == null;

    return  (pass) || (opt[i][j] != opt[i - 1][j] && parent.hasEdge(child));
  }
}
