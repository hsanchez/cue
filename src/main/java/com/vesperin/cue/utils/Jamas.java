package com.vesperin.cue.utils;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import com.google.common.base.Stopwatch;
import com.vesperin.cue.text.Word;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * @author Huascar Sanchez
 */
public class Jamas {
  private Jamas(){}

  /**
   * Gets the specified row of a matrix.
   * @param m the matrix.
   * @param row the row to get.
   * @return the specified row of m.
   */
  public static Matrix getRow(Matrix m, int row) {
    return m.getMatrix(row, row, 0, m.getColumnDimension() - 1);
  }

  /**
   * Deletes a column from a matrix.  Does not change the passed matrix.
   * @param m the matrix.
   * @param col the column to delete.
   * @return m with the specified column deleted.
   */
  public static Matrix deleteCol(Matrix m, int col) {
    int numRows = m.getRowDimension();
    int numCols = m.getColumnDimension();
    Matrix m2 = new Matrix(numRows,numCols-1);
    for (int mj=0,m2j=0; mj < numCols; mj++) {
      if (mj == col)
        continue;  // skips incrementing m2j
      for (int i=0; i<numRows; i++) {
        m2.set(i,m2j,m.get(i,mj));
      }
      m2j++;
    }
    return m2;
  }

  /**
   * Computes cosine similarity between two matrices.
   *
   * @param sourceDoc matrix a
   * @param targetDoc matrix b
   * @return cosine similarity score
   */
  public static double computeSimilarity(Matrix sourceDoc, Matrix targetDoc) {
    double dotProduct     = sourceDoc.arrayTimes(targetDoc).norm1();
    double euclideanDist  = sourceDoc.normF() * targetDoc.normF();
    return dotProduct / euclideanDist;
  }

  /**
   *
   * @param matrix
   * @return
   */
  public static Matrix buildSimilarityMatrix(Matrix matrix){
    final int numDocs = matrix.getColumnDimension();
    final Matrix similarityMatrix = new Matrix(numDocs, numDocs);
    for (int i = 0; i < numDocs; i++) {
      final Matrix sourceDocMatrix = Jamas.getCol(matrix, i);
      for (int j = 0; j < numDocs; j++) {
        final Matrix targetDocMatrix = Jamas.getCol(matrix, j);
        similarityMatrix.set(i, j,
          Jamas.computeSimilarity(sourceDocMatrix, targetDocMatrix));
      }
    }

    return similarityMatrix;
  }

  public static void printRawFreqMatrix(Matrix matrix, List<String> documentNames, List<Word> words){
    printMatrix("Raw Frequency Matrix", matrix, documentNames, words, new PrintWriter(System.out));
  }


  public static void printMatrix(String legend, Matrix matrix, List<String> documentNames, List<Word> words, PrintWriter writer) {
    writer.printf("=== %s ===%n", legend);
    writer.printf("%15s", " ");
    for (int i = 0; i < documentNames.size(); i++) {
      writer.printf("%8s", "D" + i);
    }

    writer.println();
    for (int i = 0; i < words.size(); i++) {
      writer.printf("%15s", words.get(i).getWord());
      for (int j = 0; j < documentNames.size(); j++) {
        double val = matrix.get(i, j);
        val = Double.isNaN(val) ? 0.0D : val;
        writer.printf("%8.4f", val);
      }

      writer.println();
    }

    writer.println();
    writer.println();

    for (int i = 0; i < documentNames.size(); i++) {
      writer.println(i + " - " + documentNames.get(i));
    }

    writer.flush();
  }

  public static Matrix tfidfMatrix(Matrix matrix){

    final Stopwatch start = Stopwatch.createStarted();

    // Phase 1: apply IDF weight to the raw word frequencies
    int n = matrix.getColumnDimension();
    for (int j = 0; j < matrix.getColumnDimension(); j++) {
      for (int i = 0; i < matrix.getRowDimension(); i++) {
        double matrixElement = matrix.get(i, j);

        if (matrixElement > 0.0D) {

          final Matrix subMatrix  = matrix.getMatrix(i, i, 0, matrix.getColumnDimension() - 1);
          final double dm         = countDocsWithWord(subMatrix);
          System.out.println("#tfidfMatrix: Counting docs with word: " + start);
          final double tfIdf      = matrix.get(i,j) * (1 + Math.log(n) - Math.log(dm));

          matrix.set(i, j, tfIdf);
        }
      }
    }

    System.out.println("#tfidfMatrix: IDF Weight calculation: " + start);

    // Phase 2: normalize the word scores for a single document
    for (int j = 0; j < matrix.getColumnDimension(); j++) {
      final Matrix colMatrix  = getCol(matrix, j);
      final double sum        = colSum(colMatrix);

      for (int i = 0; i < matrix.getRowDimension(); i++) {
        matrix.set(i, j, (matrix.get(i, j) / sum));
      }
    }

    System.out.println("#tfidfMatrix: Word score normalization: " + start);


    return matrix;
  }


  private static double countDocsWithWord(Matrix rowMatrix) {
    double numDocs = 0.0D;
    for (int j = 0; j < rowMatrix.getColumnDimension(); j++) {
      if (rowMatrix.get(0, j) > 0.0D) {
        numDocs++;
      }
    }

    return numDocs;
  }

  public static Matrix createQueryVector(Matrix corpus/*raw frequencies*/, List<Word> query){

    final Stopwatch start = Stopwatch.createStarted();

    // phase 1: Singular value decomposition
    final SingularValueDecomposition svd = new SingularValueDecomposition(corpus);
    final Matrix wordVector     = svd.getU();
    final Matrix sigma          = svd.getS();

    System.out.println("#createQueryVector: SVD to build query vector: " + start);

    // compute the value of k (i.e., # latent dimensions)
    final int k = (int) Math.floor(Math.sqrt(corpus.getColumnDimension()));
    double[][] queryVector = new double[k][0];

    Arrays.fill(queryVector, 0.0D);

    // populate query vector with real values
    for(int i = 0; i < query.size(); i++){
      for(int j = 0; j < k; j++){
        final double val = corpus.get(i, j);
        queryVector[i][0] += Double.isNaN(val) ? 0.0D : val;
      }
    }

    System.out.println(String.format("#createQueryVector: Populate query (%s): %s", query, start));

    final Matrix reducedWordVector = wordVector.getMatrix(
      0, wordVector.getRowDimension() - 1, 0, k - 1);

    final Matrix reducedSigma = sigma.getMatrix(0, k - 1, 0, k - 1);

    final Matrix queryMatrix = new Matrix(queryVector);

    final Matrix result      = queryMatrix.transpose().times(reducedWordVector).times(reducedSigma.inverse());
    System.out.println("#createQueryVector: Build query matrix: " + start);

    return result;
  }

  public static Matrix runLSI(Matrix matrix){
    // compute the value of k (ie where to truncate) // used to be getColDimensions
    int k = (int) Math.floor(Math.sqrt(matrix.getRowDimension()));

    // phase 1: Singular value decomposition
    final SingularValueDecomposition svd = matrix.svd();

    final Matrix wordVector     = svd.getU();
    // used to be getColDimensions instead of getRowDimension
    final Matrix sigma          = svd.getS(); //getSigma(matrix.getRowDimension(), svd.getSingularValues());
    final Matrix documentVector = svd.getV();

    // should I change for getRow? the current one is similar to getCol.
    final Matrix reducedWordVector      = getSubMatrix(wordVector, 0, wordVector.getRowDimension() - 1, 0, k - 1);
    final Matrix reducedSigma           = sigma.getMatrix(0, k - 1, 0, k - 1);
    final Matrix reducedDocumentVector  = documentVector.getMatrix(0, documentVector.getRowDimension() - 1, 0, k - 1);

    final Matrix weights = reducedWordVector.times(reducedSigma).times(reducedDocumentVector.transpose());

    // Phase 2: normalize the word score for a single document
    for (int j = 0; j < weights.getColumnDimension(); j++) {
      double sum = colSum(getCol(weights, j));

      for (int i = 0; i < weights.getRowDimension(); i++) {
        weights.set(i, j, Math.abs((weights.get(i, j)) / sum));
      }
    }

    return weights;
  }

  private static Matrix getSubMatrix(Matrix matrix, int var1, int var2, int var3, int var4) {
    Matrix var5 = new Matrix(var2 - var1 + 1, var4 - var3 + 1);
    double[][] var6 = var5.getArray();

    try {
      for(int var7 = var1; var7 < var2; ++var7) {
        for(int var8 = var3; var8 < var4; ++var8) {
          var6[var7 - var1][var8 - var3] = matrix.getArray()[var7][var8];
        }
      }

      return var5;
    } catch (ArrayIndexOutOfBoundsException var9) {
      throw new ArrayIndexOutOfBoundsException("Submatrix indices");
    }
  }

  private static Matrix getSigma(int cols, double[] singularValues){
    Matrix var1 = new Matrix(cols, cols);
    double[][] var2 = var1.getArray();

    for(int var3 = 0; var3 < cols; ++var3) {
      for(int var4 = 0; var4 < cols; ++var4) {
        var2[var3][var4] = 0.0D;
      }

      var2[var3][var3] = (var3 > singularValues.length - 1
        ? 0.00D
        : singularValues[var3]
      );
    }

    return var1;
  }


  public static Matrix lsiTransform(Matrix matrix) {
    final Stopwatch start = Stopwatch.createStarted();

    // phase 1: Singular value decomposition
    final SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
    System.out.println("#lsiTransform: SVD application: " + start);
    final Matrix wordVector     = svd.getU();
    final Matrix sigma          = svd.getS();
    final Matrix documentVector = svd.getV();

    System.out.println("#lsiTransform: W, S, D matrices: " + start);

    // compute the value of k (i.e., where to truncate)
    final int k = (int) Math.floor(Math.sqrt(matrix.getColumnDimension()));
    final Matrix reducedWordVector = wordVector.getMatrix(
      0, wordVector.getRowDimension() - 1, 0, k - 1);

    final Matrix reducedSigma = sigma.getMatrix(0, k - 1, 0, k - 1);
    final Matrix reducedDocumentVector = documentVector.getMatrix(
      0, documentVector.getRowDimension() - 1, 0, k - 1);
    final Matrix weights = reducedWordVector.times(
      reducedSigma).times(reducedDocumentVector.transpose());

    System.out.println("#lsiTransform: Truncate latent dimensions: " + start);

    // Phase 2: normalize the word scores for a single document
    for (int j = 0; j < weights.getColumnDimension(); j++) {
      double sum = colSum(getCol(weights, j));

      for (int i = 0; i < weights.getRowDimension(); i++) {
        double val = Math.abs((weights.get(i, j)) / sum);
        val = Double.isNaN(val) ? 0.0D : val;
        weights.set(i, j, val);
      }
    }

    System.out.println("#lsiTransform: Normalize word scores: " + start);

    return weights;
  }

  private static double colSum(Matrix colMatrix) {
    double sum = 0.0D;
    for (int i = 0; i < colMatrix.getRowDimension(); i++) {
      sum += colMatrix.get(i, 0);
    }

    if(Double.isNaN(sum)){
      System.out.println("Invalid sum: " + sum);
    }

    return sum;
  }

  /**
   * Gets the specified column of a matrix.
   *
   * @param m the matrix.
   * @param col the column to get.
   * @return the specified column of m.
   */
  public static Matrix getCol(Matrix m, int col) {
    return m.getMatrix(0, m.getRowDimension() - 1, col, col);
  }

  /**
   * Gets the sum of the specified row of the matrix.
   *
   * @param m the matrix.
   * @param row the row.
   * @return the sum of m[row,*]
   */
  public static double rowSum(Matrix m, int row) {
    // error check the column index
    if (row < 0 || row >= m.getRowDimension()) {
      throw new IllegalArgumentException(
        "row exceeds the row indices [0,"+(m.getRowDimension() - 1)+"] for m."
      );
    }

    double rowSum = 0;

    // loop through the rows for this column and compute the sum
    int numCols = m.getColumnDimension();
    for (int j = 0; j < numCols; j++) {
      final double val = m.get(row,j);
      rowSum += Double.isNaN(val) ? 0.0D : val;
    }

    return rowSum;
  }

  /** Multiply a matrix by a scalar, C = s*A
   @param s    scalar
   @return     s*A
   */

  public static Matrix div(Matrix matrix, double s) {
    final int m = matrix.getRowDimension();
    final int n = matrix.getColumnDimension();
    final Matrix X = new Matrix(m, n);

    final double[][] C = X.getArray();
    final double[][] A = matrix.getArray();

    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        C[i][j] = A[i][j]/s;
      }
    }
    return X;
  }
}
