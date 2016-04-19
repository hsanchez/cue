package com.vesperin.cue.text;

import com.google.common.collect.Ordering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class WordCounter {

  private static final Ordering<Map.Entry<String, Counter>> BY_FREQ_ASC = new Ordering<Map.Entry<String, Counter>>() {
    public int compare(Map.Entry<String, Counter> left, Map.Entry<String, Counter> right) {
      return left.getValue().compareTo(right.getValue());
    }
  };

  private final Set<StopWords> stopWords;
  private final Map<String, Counter> items;
  private int totalItemCount;

  /**
   * Counts words in some text.
   */
  public WordCounter(){
    this(new ArrayList<>());
  }

  /**
   * Counts words in some text.
   */
  public WordCounter(Iterable<String> items){
    this(items, EnumSet.of(StopWords.ENGLISH, StopWords.JAVA));
  }

  public WordCounter(Iterable<String> items, Set<StopWords> stopWords){
    this.stopWords      = stopWords;
    this.items          = new HashMap<>();
    this.totalItemCount = 0;

    addAll(items);
  }

  /**
   * Adds all items in iterable to this counter.
   * @param items iterable made of string items.
   */
  public void addAll(final Iterable<String> items) {
    for (final String word : items) {
      add(word);
    }
  }

  /**
   * Adds an item to this counter.
   *
   * @param item string item.
   */
  public void add(String item) {
    add(item, 1);
  }

  /**
   * Adds a fixed count of items to this counter.
   *
   * @param item string item
   * @param count number of times this item will be added.
   */
  public void add(String item, int count) {
    if(items.containsKey(item)){
      items.get(item).increment(count);
    } else {
      if(!StopWords.isStopWord(stopWords, item)){
        final Counter newCounter = new Counter(item);
        newCounter.increment(count);
        items.put(item, newCounter);
      }
    }

    totalItemCount += count;
  }



  /**
   * Combines this WordCounter with another one.
   *
   * @param wordCounter another WordCounter object.
   * @return a new and combined WordCounter
   */
  public WordCounter combine(final WordCounter wordCounter) {
    final WordCounter newWordCounter = new WordCounter();

    for (final Map.Entry<String, Counter> e : this.items.entrySet()) {
      newWordCounter.add(e.getKey(), e.getValue().value());
    }

    for (final Map.Entry<String, Counter> e : wordCounter.items.entrySet()) {
      newWordCounter.add(e.getKey(), e.getValue().value());
    }

    return newWordCounter;
  }

  /**
   * @return the total number unique items contained in this WordCounter.
   */
  public int itemsCount() {
    return totalItemCount;
  }

  /**
   * Returns the list of most frequent items.
   *
   * @param k number of results to collect.
   * @return A list of the min(k, size()) most frequent items
   */
  public List<String> mostFrequent(int k) {
    final List<Map.Entry<String, Counter>> all = entriesByFrequency();
    final int resultSize = Math.min(k, items.size());
    final List<String> result = new ArrayList<>(resultSize);

    result.addAll(all.subList(0, resultSize).stream()
      .map(Map.Entry::getKey)
      .collect(Collectors.toList()));

    return Collections.unmodifiableList(result);
  }

  /**
   * Returns the list of items (ordered by their frequency)
   *
   * @return the list of ordered items.
   */
  private List<Map.Entry<String, Counter>> entriesByFrequency() {
    final List<Map.Entry<String, Counter>> all = new ArrayList<>(
      items.entrySet()
    );

    return BY_FREQ_ASC.reverse().sortedCopy(all);
  }

  @Override public String toString() {
    return items.toString().replace("[", "(").replace("]", ")");
  }
}
