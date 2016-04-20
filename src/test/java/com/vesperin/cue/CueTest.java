package com.vesperin.cue;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.vesperin.base.Source;
import com.vesperin.base.locators.UnitLocation;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Huascar Sanchez
 */
public class CueTest {
  static final Source SRC = Source.from("Foo",
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
    final Cue cue = new Cue();
    final Set<String> expected = Sets.newHashSet(
      "file", "create", "text", "process", "code", "configuration"
    );

    final List<String> concepts = cue.assignedConcepts(SRC);
    assertEquals(concepts.size(), expected.size());

    for(String each : concepts){
      assertThat(expected.contains(each), is(true));
    }

  }

  @Test public void testCueCodeRegion() throws Exception {
    final Cue cue = new Cue();

    final List<UnitLocation> locations = cue.parse(SRC).locateMethods();
    assertThat(!locations.isEmpty(), is(true));

    final Set<String> expected = Sets.newHashSet(
      "file", "create", "text", "code", "configuration", "process"
    );

    final List<String> concepts = cue.assignedConcepts(SRC, locations.get(0));
    assertThat(!concepts.isEmpty(), is(true));

    for(String each : concepts){
      assertThat(expected.contains(each), is(true));
    }

  }

  @Test public void testTypicalityScore() throws Exception {
    final Cue cue = new Cue();

    final List<Source> typical = cue.typicalityQuery(Sources.corpus(), 1);
    final Source mostTypical = typical.get(0);

    assertEquals(mostTypical, Sources.two());
  }
}
