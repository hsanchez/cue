package com.vesperin.cue.utils;

import com.google.common.io.Files;
import com.vesperin.base.Source;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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
}
