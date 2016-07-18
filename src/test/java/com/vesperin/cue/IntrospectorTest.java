package com.vesperin.cue;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Floats;
import com.vesperin.base.Source;
import com.vesperin.cue.text.Word;
import com.vesperin.cue.utils.IO;
import com.vesperin.cue.utils.Similarity;
import com.vesperin.cue.utils.Sources;
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
public class IntrospectorTest {
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

  @Test public void testCueBasic() throws Exception {
    final Set<String> expected = Sets.newHashSet(
      "file", "create", "text", "process", "code", "configuration"
    );

    final List<Word> concepts = Cue.newIntrospector().assignedConcepts(SRC).stream().sorted().collect(Collectors.toList());

    assertEquals(concepts.size(), expected.size());

    for(Word each : concepts){
      assertThat(expected.contains(each.getWord()), is(true));
    }

    final List<Word> concepts2 = Cue.newIntrospector().assignedConcepts(Lists.newArrayList(SRC)).stream()
      .sorted().collect(Collectors.toList());

    assertEquals(concepts, concepts2);
  }

  @Test public void testCueCodeRegion() throws Exception {
    final Set<String> names = Sets.newHashSet("processFile");

    final Set<String> expected = Sets.newHashSet(
      "file", "create", "text", "code", "configuration", "process"
    );

    final Set<String> concepts = Cue.newIntrospector().assignedConcepts(SRC, names).stream()
      .map(Word::getWord)
      .collect(Collectors.toSet());
    assertThat(!concepts.isEmpty(), is(true));

    assertEquals(expected, concepts);

    final Introspector introspector = Cue.newIntrospector();
    final Set<String> c = introspector.assignedConcepts(SRC, names).stream()
      .map(Word::getWord)
      .collect(Collectors.toSet());

    assertEquals(expected, c);

    for(String each : concepts){
      assertThat(expected.contains(each), is(true));
    }

  }

  @Test public void testTypicalityScoreWithIntrospector() throws Exception {

    final Set<String>   relevant  = new HashSet<>();
    final Set<Source>   corpusSet = Corpus.getSourceFiles().stream().collect(Collectors.toSet());
    final Source        typical   = Cue.newIntrospector().typicalityQuery(1, corpusSet, relevant).stream()
      .findFirst().orElse(null);

    assertNotNull(typical);

    assertEquals(typical, Corpus.four());
  }

  @Test public void testRepresentingTypicality() throws Exception {
    final Set<Source> files = collectJavaFilesInResources().stream()
      .map(Sources::from).collect(Collectors.toSet());

    final Set<String> relevant = ImmutableSet.of("sort", "sortSet");

    final List<Source> representative = Cue.newIntrospector().representativeTypicalityQuery(files, relevant);

    assertTrue(!representative.isEmpty());
  }


  @Test public void testRepresentativeVsTypicalMeasures() throws Exception {
    final Set<Source> files = collectJavaFilesInResources().stream()
      .map(Sources::from).collect(Collectors.toSet());

    final Set<String> relevant = ImmutableSet.of("sort", "sortSet");

    final Introspector introspector   = Cue.newIntrospector();

    final List<Source> represent  = introspector.representativeTypicalityQuery(files, relevant);
    final List<Source> typical    = introspector.typicalityQuery(represent.size(), files, relevant);

    assertEquals(represent.size(), typical.size());

  }

  @Test public void testTypicalityScore() throws Exception {
    final Set<String>   relevant     = new HashSet<>();
    final Set<Source>   corpusSet    = Corpus.getSourceFiles().stream().collect(Collectors.toSet());
    final List<Source>  typical      = Cue.newIntrospector().typicalityQuery(1, corpusSet, relevant);
    final Source        mostTypical  = typical.stream().findFirst().orElse(null);

    assertNotNull(mostTypical);

    assertEquals(mostTypical, Corpus.four());
  }

  @Test public void testMostTypicalSortingImplementation() throws Exception {
    final Set<Source> files = collectJavaFilesInResources().stream()
      .map(Sources::from).collect(Collectors.toSet());

    assertThat(!files.isEmpty(), is(true));

    final Set<String> relevant = new HashSet<>();

    final List<Source> typical = Cue.newIntrospector().typicalityQuery(1, files, relevant);

    assertThat(!typical.isEmpty(), is(true));
  }

  @Test public void testMostTypicalSortingWithDiffBandwidth() throws Exception {
    final Set<Source> files = collectJavaFilesInResources().stream()
      .map(Sources::from).collect(Collectors.toSet());

    assertThat(!files.isEmpty(), is(true));

    final Set<String> relevant = ImmutableSet.of("sort", "sortStack", "sortSet");
    final List<Source> typical = Cue.newIntrospector().typicalityQuery(1, 0.3, files, relevant);

    assertThat(!typical.isEmpty(), is(true));
  }

  @Test public void testMostFrequentConcept() throws Exception {
    final List<Source> files = collectJavaFilesInResources().stream()
      .map(Sources::from).collect(Collectors.toList());

    final List<Word> concepts = Cue.newIntrospector().assignedConcepts(files);
    final List<Word> c = Cue.newIntrospector().assignedConcepts(files);

    assertThat(concepts.isEmpty(), is(false));
    assertThat(c.isEmpty(), is(false));

  }

  private static List<File> collectJavaFilesInResources() {
    return IO.collectFiles(Paths.get(IntrospectorTest.class.getResource("/").getPath()), "java");
  }


  @Test public void testCommutativePropertyOfSimilarity() throws Exception {

    assertThat(
      Floats.compare(
        Similarity.similarityScore("text", "txt"),
        Similarity.similarityScore("txt", "text")
      ) == 0, is(true));

    assertThat(
      Floats.compare(
        Similarity.normalizeDistance("text", "txt"),
        Similarity.normalizeDistance("txt", "text")
      ) == 0, is(true));
  }
}
