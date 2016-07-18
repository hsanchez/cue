package com.vesperin.cue.spi;

import Jama.Matrix;
import com.google.common.collect.ImmutableMultiset;
import com.vesperin.cue.text.Word;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Huascar Sanchez
 */
public class Kmeans {

  public static List<Cluster> cluster(Words collection) {
    int numDocs = collection.size();
    int numClusters = (int) Math.floor(Math.sqrt(numDocs));
    final List<Word> initialClusters = new ArrayList<>();

    for(int c = 0; c < numClusters; c++){
      initialClusters.add(collection.getWord(c));
    }


    // build initial clusters
    List<Cluster> clusters = new ArrayList<>();
    for (int i = 0; i < numClusters; i++) {
      Cluster cluster = new Cluster();
      cluster.addWord(initialClusters.get(i), collection.getWordVector(initialClusters.get(i)));
      clusters.add(cluster);
    }

    List<Cluster> prevClusters = new ArrayList<>();
    for (;;) {
      for (int i = 0; i < numClusters; i++) {
        clusters.get(i).getCentroid();
      }

      for (int i = 0; i < numDocs; i++) {
        int bestCluster = 0;
        double maxSimilarity = Double.MIN_VALUE;
        Matrix document = collection.getWordVector(i);
        Word docName = collection.getWord(i);

        for (int j = 0; j < numClusters; j++) {
          double similarity = clusters.get(j).getSimilarity(document);
          if (similarity > maxSimilarity) {
            bestCluster = j;
            maxSimilarity = similarity;
          }
        }

        clusters.stream()
          .filter(cluster -> cluster.getWordMatrix(docName) != null)
          .forEach(cluster -> cluster.removeWord(docName));

        clusters.get(bestCluster).addWord(docName, document);
      }


      if(ImmutableMultiset.copyOf(clusters).equals(ImmutableMultiset.copyOf(prevClusters))){
        break;
      }

      prevClusters.clear();
      prevClusters.addAll(clusters);
    }


    return clusters;
  }

}
