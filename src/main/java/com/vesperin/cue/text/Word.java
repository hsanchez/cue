package com.vesperin.cue.text;

import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class Word implements Comparable<Word> {
  private final String  word;
  private final Counter weight;
  private final Map<String, Set<String>> sourceMap;

  /**
   * Construct a new word object using a default counter.
   *
   * @param word actual word
   */
  public Word(String word){
    this(word, new Counter(word));
  }

  /**
   * Constructs a new Word
   *
   * @param word the actual word
   * @param weight the word's weight
   */
  public Word(String word, Counter weight){
    this.word   = word;
    this.weight = weight;
    this.sourceMap = new HashMap<>();
  }

  public void add(String source){
    if(Objects.isNull(source)) return;

    if(sourceMap.containsKey(word)){
      sourceMap.get(word).add(source);
    } else {
      final Set<String> value = Sets.newHashSet(source);
      sourceMap.put(word, value);
    }
  }

  @Override public int compareTo(Word o) {
    return getWord().compareTo(o.getWord());
  }

  @Override public boolean equals(Object obj) {
    return obj instanceof Word
      && getWord().equals(((Word) obj).getWord());

  }

  /**
   * @return the captured word
   */
  public String getWord(){
    return word;
  }

  /**
   *
   * @return the word's weight
   */
  public int getWeight(){
    return weight.value();
  }

  @Override public int hashCode() {
    return 89 * Objects.hash(getWord());
  }

  public int count(){
    return count(1);
  }

  public int count(int delta){
    weight.count(delta);
    return delta;
  }

  @Override public String toString() {
    return getWord() + " (" + getWeight() + ")";
  }
}
