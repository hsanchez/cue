package com.vesperin.cue;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.primitives.Floats;
import com.vesperin.base.Source;
import com.vesperin.cue.utils.IO;
import com.vesperin.cue.utils.Sources;
import com.vesperin.text.Grouping;
import com.vesperin.text.Grouping.Group;
import com.vesperin.text.Grouping.Groups;
import com.vesperin.text.Selection;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.utils.Similarity;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Huascar Sanchez
 */
public class TypicalityTest {
  private static final Source SRC = Source.from("Foo",
    Joiner.on("\n").join(
      ImmutableList.of(
        "public class Foo {"
        , " public File processFile(){"
        , "   int x = new ConfigCode().consumeException;"
        , "   try {"
        , "     // exit if x is zero or negative"
        , "     if(x <= 0) {"
        , "       throw new IllegalArgumentException();"
        , "     }"
        , "   } catch(IllegalArgumentException e){"
        , "     System.err.println(1);"
        , "   }"
        , "   "
        , "   return createTxtFile();"
        , " }"
        , " "
        , " public File createTxtFile(){"
        , "   return new File(\"/foo.txt\");"
        , " }"
        , " "
        , " public static class ConfigCode {"
        , "   final int consumeException = 1;"
        , " }"
        , "}"
      )
    )
  );

  @Test public void testConceptAnalysis() throws Exception {
    Set<Source> set = ImmutableSet.of(Corpus.five(), Corpus.four(), Corpus.two());

    try {
      final List<Word> words = Selection.selects(100, set);
      final Groups groups = Grouping.formWordGroups(words);
      final Group group = Iterables.get(groups, 0);

      assertNotNull(group);

    } catch (Throwable e){
      System.out.println(e.getLocalizedMessage());
    }
  }

  @Test public void testCueBasic() throws Exception {

    final Set<String> expected = Sets.newHashSet(
      "println", "create", "text", "code", "configuration", "process"
    );

    final List<Word> concepts = Selection.selects(200, Sets.newHashSet(SRC), StopWords.ENGLISH, StopWords.JAVA);

    assertEquals(concepts.size(), expected.size());

    for( Word each : concepts){
      assertTrue(expected.contains(each.element()));
    }

  }


  @Test public void testTypicalityScoreWithIntrospector() throws Exception {

    final Set<String>   relevant    = new HashSet<>();
    final Set<Source>   corpusSet   = Corpus.getSourceFiles().stream().collect(Collectors.toSet());
    final Typicality    typicality  = Typicality.creates();
    final Source        typical     = typicality.typicalOf(1, corpusSet, relevant).stream()
      .findFirst().orElse(null);

    assertNotNull(typical);

    assertEquals(typical, Corpus.four());
  }

  @Test public void testRepresentingTypicality() throws Exception {
    final Set<Source> files = collectJavaFilesInResources().stream()
      .map(Sources::from).collect(Collectors.toSet());

    final Set<String> relevant = ImmutableSet.of("sort", "sortSet");

    final List<Source> representative = Typicality.creates().bestOf(files, relevant);

    assertTrue(!representative.isEmpty());
  }


  @Test public void testRepresentativeVsTypicalMeasures() throws Exception {
    final Set<Source> files = collectJavaFilesInResources().stream()
      .map(Sources::from).collect(Collectors.toSet());

    final Set<String> relevant = ImmutableSet.of("sort", "sortSet");

    final Typicality typicality = Typicality.creates();

    final List<Source> represent  = typicality.bestOf(files, relevant);
    final List<Source> typical    = typicality.typicalOf(represent.size(), files, relevant);

    assertEquals(represent.size(), typical.size());

  }

  @Test public void testTypicalityScore() throws Exception {
    final Set<String>   relevant     = new HashSet<>();
    final Set<Source>   corpusSet    = Corpus.getSourceFiles().stream().collect(Collectors.toSet());
    final List<Source>  typical      = Typicality.creates().typicalOf(1, corpusSet, relevant);
    final Source        mostTypical  = typical.stream().findFirst().orElse(null);

    assertNotNull(mostTypical);

    assertEquals(mostTypical, Corpus.four());
  }

  @Test public void testMostTypicalSortingImplementation() throws Exception {
    final Set<Source> files = collectJavaFilesInResources().stream()
      .map(Sources::from).collect(Collectors.toSet());

    assertThat(!files.isEmpty(), is(true));

    final Set<String> relevant = new HashSet<>();

    final List<Source> typical = Typicality.creates().typicalOf(1, files, relevant);

    assertThat(!typical.isEmpty(), is(true));
  }

  @Test public void testMostTypicalSortingWithDiffBandwidth() throws Exception {
    final Set<Source> files = collectJavaFilesInResources().stream()
      .map(Sources::from).collect(Collectors.toSet());

    assertThat(!files.isEmpty(), is(true));

    final Set<String> relevant = ImmutableSet.of("sort", "sortStack", "sortSet");
    final List<Source> typical = Typicality.creates().typicalOf(1, 0.3, files, relevant);

    assertThat(!typical.isEmpty(), is(true));
  }

  @Test public void testMostFrequentConcept() throws Exception {
    final Set<Source> files = collectJavaFilesInResources().stream()
      .map(Sources::from).collect(Collectors.toSet());

    final List<Word> concepts = Selection.selects(200, files);

    assertThat(concepts.isEmpty(), is(false));

  }

  private static List<File> collectJavaFilesInResources() {
    return IO.collectFiles(Paths.get(TypicalityTest.class.getResource("/").getPath()), "java");
  }


  @Test public void testCommutativePropertyOfSimilarity() throws Exception {

    assertThat(
      Floats.compare(
        Similarity.similarityScore("text", "txt"),
        Similarity.similarityScore("txt", "text")
      ) == 0, is(true));
  }
}
