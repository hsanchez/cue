package com.vesperin.cue.text;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Huascar Sanchez
 */
public class WordIterator implements Iterator<String>, Iterable<String> {

  private static final String   LETTER  = "[@+\\p{javaLetterOrDigit}]";
  // TO
  private static final String   JOINER  = "[-.:/'’\\p{M}\\u2032\\u00A0\\u200C\\u200D~]";
  private static final Pattern  WORD    = Pattern.compile(LETTER + "+(" + JOINER + "+" + LETTER + "+)*");

  private final Matcher wordMatcher;
  private boolean hasNext;

  /**
   * Iterates over each word in the text.
   *
   * @param text the text this WordIterator will iterate.
   */
  public WordIterator(String text){
    this.wordMatcher = WORD.matcher(text == null ? "" : text);
    hasNext = wordMatcher.find();
  }

  @Override public boolean hasNext() {
    return hasNext;
  }

  @Override public Iterator<String> iterator() {
    return this;
  }

  @Override public String next() {
    if (!hasNext) {
      throw new NoSuchElementException();
    }
    final String s = wordMatcher.group().toLowerCase();
    hasNext = wordMatcher.find();
    return s;
  }
}
