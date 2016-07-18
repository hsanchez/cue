package com.vesperin.cue.text;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.vesperin.cue.spi.Inflector;
//import com.vesperin.cue.spi.In flector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class WordCounter {

  private static final Ordering<Word> BY_FREQ_ASC =
    new Ordering<Word>() {
      public int compare(Word left, Word right) {
        return Ints.compare(left.getWeight(), right.getWeight());
      }
    };

  private final Set<StopWords>  stopWords;
  private final Map<Word, Word> items;
  private final AtomicInteger   totalItemCount;

  /**
   * Counts words in some text.
   */
  public WordCounter(){
    this(null);
  }

  /**
   * Counts words in some text.
   */
  public WordCounter(List<Word> items){
    this(items, EnumSet.of(StopWords.ENGLISH, StopWords.JAVA));
  }

  /**
   * Counts words in some list, paying attention to a set of stop words..
   * @param items items to be counted
   * @param stopWords set of stop words
   */
  private WordCounter(List<Word> items, Set<StopWords> stopWords){
    this.stopWords      = stopWords;
    this.items          = new HashMap<>();
    this.totalItemCount = new AtomicInteger(0);

    addAll(items);
  }

  /**
   * Adds all items in iterable to this counter.
   * @param items iterable made of string items.
   */
  public void addAll(final List<Word> items) {
    final Stopwatch timer = Stopwatch.createStarted();
    if(Objects.isNull(items)) return;
    if(items.contains(null))  return;

    for( Word each : items){
      if(Objects.isNull(each)) continue;

      add(each);
    }

    System.out.println("#addAll: add all words " + timer);
  }

  private static <T> void parEach(List<T> a, Consumer<T> action) {
    if(Objects.isNull(a)) return;
    if(Objects.isNull(action)) return;
    final Spliterator<T> s = a.spliterator();
    long targetBatchSize = s.estimateSize() / (ForkJoinPool.getCommonPoolParallelism() * 8);
    new ParEach<>(null, s, action, targetBatchSize).invoke();
  }

  /**
   * Adds an item to this counter.
   *
   * @param item string item.
   */
  public void add(Word item) {
    if(item == null) return;
    if(StopWords.isStopWord(stopWords, item.getWord())) return;

    if(item.getWord().equals("id")){
      System.out.println();
    }

    if(items.containsKey(item)){
      addEntry(item);
    } else {
      final Word singular = new Word(Inflector.singularOf(item.getWord()));
      if(items.containsKey(singular)){
        if(StopWords.isStopWord(stopWords, singular.getWord())) return;
        addEntry(singular);
      } else {
        add(singular, singular.count());
      }
    }
  }

  private void addEntry(Word item) {
    final Word entry = items.remove(item);
    if(entry == null) {
      add(item, item.count());
    } else {
      add(entry, entry.count());
    }
  }

  /**
   * Adds a fixed count of items to this counter.
   *
   * @param item string item
   * @param count number of times this item will be added.
   */
  public void add(Word item, int count) {
    items.put(item, item);
    totalItemCount.addAndGet(count);
  }

  /**
   * @return the list of values seen by this counter.
   */
  public List<Word> getWords(){
    return items.values().stream().collect(Collectors.toList());
  }


  /**
   * Combines this WordCounter with another one.
   *
   * @param wordCounter another WordCounter object.
   * @return a new and combined WordCounter
   */
  public WordCounter combine(final WordCounter wordCounter) {
    final WordCounter newWordCounter = new WordCounter();

    this.getWords().forEach(newWordCounter::add);
    wordCounter.getWords().forEach(newWordCounter::add);

    return newWordCounter;
  }

  /**
   * @return the total number unique items contained in this WordCounter.
   */
  public int itemsCount() {
    return totalItemCount.get();
  }

  /**
   * Returns the list of most frequent items.
   *
   * @param k number of results to collect.
   * @return A list of the min(k, size()) most frequent items
   */
  public List<Word> mostFrequent(int k) {
    final List<Word> all = entriesByFrequency();
    final int resultSize = Math.min(k, items.size());
    final List<Word> result = new ArrayList<>(resultSize);

    result.addAll(all.subList(0, resultSize).stream()
      .collect(Collectors.toList()));

    return Collections.unmodifiableList(result);
  }

  /**
   * Returns the list of items (ordered by their frequency)
   *
   * @return the list of ordered items.
   */
  private List<Word> entriesByFrequency() {
    final List<Word> words = items.entrySet().stream()
      .map(Map.Entry::getValue)
      .collect(Collectors.toList());

    return BY_FREQ_ASC.reverse().sortedCopy(words);
  }

  @Override public String toString() {
    return itemsCount() + " found: "
      + items.toString().replace("[", "(").replace("]", ")");
  }

  private static class ParEach<T> extends CountedCompleter<Void> {
    final Spliterator<T> spliterator;
    final Consumer<T> action;
    final long targetBatchSize;

    ParEach(ParEach<T> parent, Spliterator<T> spliterator,
            Consumer<T> action, long targetBatchSize) {
      super(parent);
      this.spliterator = spliterator;
      this.action = action;
      this.targetBatchSize = targetBatchSize;
    }

    public void compute() {
      Spliterator<T> sub;
      while (spliterator.estimateSize() > targetBatchSize &&
        (sub = spliterator.trySplit()) != null) {
        addToPendingCount(1);
        new ParEach<>(this, sub, action, targetBatchSize).fork();
      }
      spliterator.forEachRemaining(action);
      propagateCompletion();
    }
  }
}
