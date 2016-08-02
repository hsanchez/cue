package com.vesperin.cue.utils;

import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class IO {

  private IO(){
    throw new Error("Utility class");
  }

  /**
   * Reads all lines in a file.
   *
   * @param filepath the path of the file to read.
   * @return a list of lines in the file.
   */
  public static List<String> readLines(Path filepath){
    try {
      return Files.readAllLines(filepath, Charset.defaultCharset());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Collect files in a given location.
   *
   * @param path the path to the directory to access
   * @param extension extension of files to collect
   * @param keywords hints which files to ignore (based on their names)
   * @return the list of files matching a given extension.
   */
  public static List<File> collectFiles(Path path, String extension, String... keywords){
    return collectFiles(path.toFile(), extension, keywords)
      .stream()
      .collect(Collectors.toList());
  }

  /**
   * Collect files in a given location.
   *
   * @param path the path to the directory to access
   * @param extension extension of files to collect
   * @return the list of files matching a given extension.
   */
  public static List<File> collectFiles(Path path, String extension){
    return collectFiles(path.toFile(), extension);
  }

  /**
   * Collect files in a given location.
   *
   * @param directory directory to access
   * @param extension extension of files to collect
   * @return the list of files matching a given extension.
   */
  private static List<File> collectFiles(File directory, String extension, String... keywords){
    final List<File> data = new ArrayList<>();

    try {
      IO.collectDirContent(directory, extension, data, keywords);
    } catch (IOException e) {
      // ignored
    }

    return data;
  }


  private static void collectDirContent(final File classDir, final String extension, final Collection<File> files, final String... keywords) throws IOException {

    final Path start   = Paths.get(classDir.toURI());
    final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*." + extension);

    final Set<String> blackSet = Sets.newHashSet(Arrays.asList(keywords));
    final Predicate<File> inTheClub = inTheClub(blackSet);

    try {
      Files.walkFileTree(start, new SimpleFileVisitor<Path>(){
        @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws
          IOException {

          final Path fileName = file.getFileName();
          if(matcher.matches(fileName)){
            final File visitedFile = file.toFile();

            final String name = visitedFile.getName().replace(".java", "");
            if(!blackSet.contains(name) && inTheClub.test(visitedFile)){
              files.add(visitedFile);
            }
          }

          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException ignored){}

  }

  private static Predicate<File> inTheClub(Set<String> blackSet){
    return e -> {
      for(String each : blackSet){
        final String name = e.getName().replace(".java", "");
        if(name.contains(each)) return false;
      }

      return true;
    };
  }
}
