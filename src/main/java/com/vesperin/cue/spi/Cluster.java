package com.vesperin.cue.spi;

import Jama.Matrix;
import com.google.common.base.Joiner;
import com.vesperin.cue.text.Word;
import com.vesperin.cue.utils.Jamas;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class Cluster {
  private final Map<Word, Matrix> wordToMatrix;
  private final List<Word>        words;

  private Matrix centroid = null;

  public Cluster() {
    this.wordToMatrix = new LinkedHashMap<>();
    this.words = new LinkedList<>();
  }

  public Set<Word> getWords() {
    return wordToMatrix.keySet();
  }

  public Word getWord(int pos) {
    return words.get(pos);
  }

  public Matrix getWordMatrix(Word word) {
    return wordToMatrix.get(word);
  }

  public Matrix getWordMatrix(int pos) {
    return wordToMatrix.get(words.get(pos));
  }

  public void addWord(Word word, Matrix wordMatrix) {
    wordToMatrix.put(word, wordMatrix);
    words.add(word);
  }

  public void removeWord(Word word) {
    wordToMatrix.remove(word);
    words.remove(word);
  }

  public int size() {
    return wordToMatrix.size();
  }

  public boolean contains(Word word) {
    return wordToMatrix.containsKey(word);
  }

  /**
   * Returns a document (term vector) consisting of the average of the
   * coordinates of the documents in the cluster. Returns a null Matrix
   * if there are no documents in the cluster.
   * @return the centroid of the cluster, or null if no documents have
   * been added to the cluster.
   */
  public Matrix getCentroid() {
    if (wordToMatrix.size() == 0) {
      return null;
    }

    final Matrix d = wordToMatrix.get(words.get(0));
    centroid = new Matrix(d.getRowDimension(), d.getColumnDimension());

    for (Word each : wordToMatrix.keySet()) {
      final Matrix wordMatrix = wordToMatrix.get(each);
      centroid = centroid.plus(wordMatrix);
    }

    centroid = centroid.times(1.0D / wordToMatrix.size());
    return centroid;
  }

  /**
   * Returns the radius of the cluster. The radius is the average of the
   * square root of the sum of squares of its constituent document term
   * vector coordinates with that of the centroid.
   * @return the radius of the cluster.
   */
  public double getRadius() {
    double radius = 0.0D;
    if (centroid != null) {
      for (Word each : words) {
        Matrix doc = getWordMatrix(each);
        radius += doc.minus(centroid).normF();
      }
    }
    return radius / words.size();
  }

  /**
   * Returns the Euclidean distance between the centroid of this cluster
   * and the new document.
   * @param doc the document to be measured for distance.
   * @return the euclidean distance between the cluster centroid and the
   * document.
   */
  public double getEuclidianDistance(Matrix doc) {
    if (centroid != null) {
      return (doc.minus(centroid)).normF();
    }

    return 0.0D;
  }

  /**
   * Returns the maximum distance from the specified document to any of
   * the documents in the cluster.
   * @param doc the document to be measured for distance.
   * @return the complete linkage distance from the cluster.
   */
  public double getCompleteLinkageDistance(Matrix doc) {
    if (wordToMatrix.size() ==0) { return 0.0D; }

    final double[] distances = new double[wordToMatrix.size()];

    for (int i = 0; i < distances.length; i++) {
      Matrix clusterDoc = wordToMatrix.get(words.get(i));
      distances[i] = clusterDoc.minus(doc).normF();
    }

    final OptionalDouble optionalDouble = Arrays.stream(distances).max();

    assert optionalDouble.isPresent();

    return optionalDouble.getAsDouble();
  }

  /**
   * Returns the cosine similarity between the centroid of this cluster
   * and the new document.
   * @param doc the document to be measured for similarity.
   * @return the similarity of the centroid of the cluster to the document.
   */
  public double getSimilarity(Matrix doc) {
    if (centroid != null) {
      return Jamas.computeSimilarity(centroid, doc);
    }
    return 0.0D;
  }

  @Override public boolean equals(Object obj) {
    if (!(obj instanceof Cluster)) {
      return false;
    }

    Cluster that = (Cluster) obj;

    Word[] thisDocNames = this.getWords().toArray(new Word[0]);
    Word[] thatDocNames = that.getWords().toArray(new Word[0]);

    if (thisDocNames.length != thatDocNames.length) {
      return false;
    }

    Arrays.sort(thisDocNames);
    Arrays.sort(thatDocNames);

    return Arrays.equals(thisDocNames, thatDocNames);
  }

  @Override public int hashCode() {
    Word[] docNames = getWords().toArray(new Word[0]);
    Arrays.sort(docNames);
    return Joiner.on(",").join(docNames).hashCode();
  }

  @Override public String toString() {
    return wordToMatrix.keySet().toString();
  }
}
