package com.vesperin.cue.spi;

import Jama.Matrix;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import com.vesperin.cue.text.Word;
import com.vesperin.cue.utils.Jamas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @author Huascar Sanchez
 */
public class Words implements Iterable<Word> {
  private final Matrix matrix;
  private final Map<Word, Matrix> wordToMatrixMap;
  private final List<Word> words;

  /**
   * Constructs a new Words repo.
   *
   * @param matrix LSI matrix
   * @param words list of words
   */
  public Words(Matrix matrix, List<Word> words){
    this.matrix           = matrix;
    this.wordToMatrixMap  = new HashMap<>();
    this.words            = new ArrayList<>();

    int position = 0; for (Word each : words) {
      this.wordToMatrixMap.put(each, Jamas.getRow(matrix, position));
      this.words.add(each);
      position++;
    }
  }

  @Override public Iterator<Word> iterator() {
    return getWords().iterator();
  }

  public int size() {
    return wordToMatrixMap.keySet().size();
  }

  public List<Word> getWords() {
    return words;
  }

  public Word getWord(int position) {
    return words.get(position);
  }

  public Matrix getWordVector(int position) {
    return wordToMatrixMap.get(words.get(position));
  }

  public Matrix getWordVector(Word documentName) {
    return wordToMatrixMap.get(documentName);
  }

  public void shuffle() {
    Collections.shuffle(words);
  }

  public Map<List<Word>/*neighbors*/, Double> getSimilarityMap() {
    final Map<List<Word>,Double> similarityMap = new HashMap<>();
    final Matrix similarityMatrix = Jamas.buildSimilarityMatrix(matrix);

    for (int i = 0; i < similarityMatrix.getRowDimension(); i++) {
      for (int j = 0; j < similarityMatrix.getColumnDimension(); j++) {
        Word sourceDoc = getWord(i);
        Word targetDoc = getWord(j);

        similarityMap.put(Lists.newArrayList(sourceDoc, targetDoc),
          similarityMatrix.get(i, j)
        );
      }
    }

    return similarityMap;
  }

  public List<Word> getNeighbors(Word docName, Map<List<Word>, Double> similarityMap, int numNeighbors) {
    if (numNeighbors > size()) {
      throw new IllegalArgumentException(
        "numNeighbors too large, max: " + size());
    }

    final Map<List<Word>, Double> differenceMap = new HashMap<>();
    List<Word> neighbors = new ArrayList<>();
    neighbors.addAll(getWords());

    for (Word documentName : getWords()) {
      final List<Word> keys = Lists.newArrayList(docName, documentName);
      final double difference = Math.abs(similarityMap.get(keys) - 1.0D);
      differenceMap.put(Lists.newArrayList(documentName), difference);
    }

    final Stream<Word> wordStream = neighbors.stream()
      .sorted(
        (a, b) -> Doubles.compare(
          differenceMap.get(Lists.newArrayList(a)), differenceMap.get(Lists.newArrayList(b))
        )
      );

    neighbors = wordStream.collect(toList());

    return neighbors.subList(0, numNeighbors + 1);
  }

}
