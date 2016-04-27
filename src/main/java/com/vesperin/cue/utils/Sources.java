package com.vesperin.cue.utils;

import com.google.common.io.Files;
import com.vesperin.base.Source;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class Sources {
  private Sources(){
    throw new Error("Cannot be instantiated");
  }

  /**
   * Converts a file into a source object.
   *
   * @param file the file to be converted.
   * @return a new source code object.
   */
  public static Source from(File file) {
    try {
      final String name     = Files.getNameWithoutExtension(file.getName());
      final String content  = Files.readLines(file, Charset.defaultCharset()).stream()
        .collect(Collectors.joining("\n"));

      return Source.from(name, content);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Converts a list of files into a list of source objects.
   *
   * @param files the files to be converted
   * @return the list source objects.
   */
  public static List<Source> from(List<File> files) {
    return files.stream().map(Sources::from).collect(Collectors.toList());
  }

  /**
   * Populates a corpus with a list of entries.
   *
   * @param corpus corpus to populate.
   * @param withEntries entries to parse.
   * @return set of relevant methods
   */
  public static Set<String> populate(List<Source> corpus, List<String> withEntries) {
    final Set<String> relevant = new HashSet<>();
    for (String entry : withEntries){

      final String[] parts = Signatures.entrySignature(entry);

      if(Signatures.isSingle(parts)){
        final Path eachPath = Paths.get(parts[0]);
        corpus.add(Sources.from(eachPath.toFile()));
      } else {
        final Path eachPath = Paths.get(parts[0]);
        corpus.add(Sources.from(eachPath.toFile()));

        final String methodInfo = parts[1];
        relevant.add(Signatures.methodSignature(methodInfo)[0]);
      }
    }

    return relevant;
  }
}
