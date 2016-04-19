package com.vesperin.cue.text;

import com.google.common.collect.Sets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.primitives.Floats.compare;
import static com.vesperin.cue.text.WordCorrector.isNumber;

/**
 * @author Huascar Sanchez
 */
public enum SpellChecker implements WordCorrector {
  INSTANCE(loadFile());

  private static final String CAMEL_CASE = "((?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z]))|_";

  private SortedMap<String,Integer> dictionary;

  SpellChecker(Path index){
    this.dictionary = new TreeMap<>();

    try {
      populateDictionary(index, this.dictionary);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to populate dictionary!");
    }
  }


  public static SpellChecker getInstance(){
    return SpellChecker.INSTANCE;
  }

  /**
   * Suggests a correction for a given word.
   *
   * @param word the string to be corrected.
   * @return a suggested correction.
   */
  public static String suggestCorrection(String word){
    return SpellChecker.getInstance().correct(word);
  }

  public static boolean onlyConsonants(String word){
    return WordCorrector.onlyConsonants(word);
  }

  public static float similarity(String word, String suggestion){
    return WordCorrector.similarity(word, suggestion);
  }

  public static boolean isStopWord(Set<StopWords> stops, String word){
    return WordCorrector.isStopWord(stops, word);
  }

  @Override public String correct(String word, float accuracy) {

    if(contains(word)) { return word; } else {
      if(onlyConsonants(word)) {

        // Make some edits
        final Optional<String> e1 = max(captureThoseInDictionary(mutate(word)));
        final Optional<String> e2 = max(captureThoseInDictionary(mutate(word)
          .map(this::mutate).flatMap((x)->x))
        );

        // Use prefixes (like a Trie)
        Optional<String> e3 = max(getPrefixedBy(word, dictionary).stream());
        if(!e3.isPresent()){
          e3 = max(getPrefixedBy(word.substring(0, word.length() - 1), dictionary).stream());
        }

        final Set<String> winners = new HashSet<>();
        if(e1.isPresent()) winners.add(e1.get());
        if(e2.isPresent()) winners.add(e2.get());
        if(e3.isPresent()) winners.add(e3.get());

        final Optional<String> winner = winners.stream().max(
          (a, b) -> compare(similarity(word, a), similarity(word, b))
        );

        if(winner.isPresent()) return winner.get();

        return word;
      } else {
        Optional<String> e0 = max(getPrefixedBy(word, dictionary).stream());

        if(!e0.isPresent()){
          e0 = max(getPrefixedBy(word.substring(0, word.length() - 1), dictionary).stream());
        }

        if(e0.isPresent()) return e0.get();

        return word;
      }
    }
  }


  private Stream<String> captureThoseInDictionary(Stream<String> words){
    return words.filter(this::contains);
  }

  public static boolean containsWord(String word){
    return SpellChecker.getInstance().contains(word);
  }

  private boolean contains(String word){
    return dictionary.containsKey(word);
  }

  private static <V> SortedMap<String, V> filterPrefix(SortedMap<String,V> baseMap, String prefix) {
    if(prefix.length() > 0) {
      char nextLetter = (char)(prefix.charAt(prefix.length() - 1) + 1);
      String end = prefix.substring(0, prefix.length() - 1) + nextLetter;
      return baseMap.subMap(prefix, end);
    }
    return baseMap;
  }

  private static <V> Set<String> getPrefixedBy(String word, SortedMap<String, V> baseMap){
    return filterPrefix(baseMap, word).keySet();
  }

  private static Path loadFile(){
    try {
      return Paths.get((WordCorrector.class.getResource("/big.txt").toURI()));
    } catch (Exception e){
      return null;
    }
  }


  private Optional<String> max(Stream<String> stream){
    return stream.max((a, b) -> dictionary.get(a) - dictionary.get(b));
  }

  private Stream<String> mutate(final String word){
    final Stream<String> deletes    = IntStream.range(0, word.length())
      .mapToObj((i) -> word.substring(0, i) + word.substring(i + 1));

    final Stream<String> replaces   = IntStream.range(0, word.length())
      .mapToObj((i)->i)
      .flatMap( (i) -> "abcdefghijklmnopqrstuvwxyz".chars()
        .mapToObj( (c) -> word.substring(0,i) + (char)c + word.substring(i+1)));

    final Stream<String> inserts    = IntStream.range(0, word.length()+1)
      .mapToObj((i)->i)
      .flatMap( (i) -> "abcdefghijklmnopqrstuvwxyz".chars()
        .mapToObj( (c) ->  word.substring(0,i) + (char)c + word.substring(i)));

    final Stream<String> transposes = IntStream.range(0, word.length()-1)
      .mapToObj((i)-> word.substring(0,i)
        + word.substring(i + 1,i + 2)
        + word.charAt(i) + word.substring(i + 2));

    return Stream.of( deletes,replaces,inserts,transposes ).flatMap((x)->x);
  }


  private static void populateDictionary(Path dictionaryFile, SortedMap<String, Integer> dict) throws
    IOException {
    Objects.requireNonNull(dict);
    Objects.requireNonNull(dictionaryFile);

    final Set<StopWords> stops = Sets.newHashSet(StopWords.ENGLISH, StopWords.JAVA);

    final List<String> lines = Files.readAllLines(dictionaryFile);

    Pattern p = Pattern.compile("\\w+");
    for(String line : lines){
      final Matcher m = p.matcher(line);

      while(m.find()) {

        final String[] words = m.group().split(CAMEL_CASE);

        for(String each : words){
          if(each.length() <= 2)      continue;
          if(onlyConsonants(each))    continue;
          if(isNumber(each))          continue;
          if(isStopWord(stops, each)) continue;

          final String lowerCase = each.toLowerCase();
          dict.put(
            lowerCase,
            // increase frequency
            (dict.containsKey(lowerCase) ? dict.get(lowerCase) + 1 : 1)
          );
        }
      }
    }
  }
}
