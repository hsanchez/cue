package com.vesperin.cue.text;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Huascar Sanchez
 */
public class SpellCheckingTest {

  @Test public void testSpellChecking() throws Exception {
    final Set<String> corrections = new HashSet<>();
    corrections.add("configuration");
    corrections.add("text");
    corrections.add("error");
    corrections.add("string");

    final Set<String> words = new HashSet<>();
    words.add("txt");
    words.add("config");
    words.add("err");
    words.add("str");

    for(String each : words){
      final String correction = SpellChecker.suggestCorrection(each);
      assertThat(corrections.contains(correction), is(true));
    }
  }

}
