package com.vesperin.cue.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Huascar Sanchez
 */
public class IO {

  private IO(){
    throw new Error("Utility class");
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
  private static List<File> collectFiles(File directory, String extension){
    final List<File> data = new ArrayList<>();

    try {
      IO.collectDirContent(directory, extension, data);
    } catch (IOException e) {
      // ignored
    }

    return data;
  }


  private static void collectDirContent(final File classDir, final String extension, final Collection<File> files) throws IOException {

    final Path start   = Paths.get(classDir.toURI());
    final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*." + extension);

    try {
      Files.walkFileTree(start, new SimpleFileVisitor<Path>(){
        @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws
          IOException {


          final Path fileName = file.getFileName();
          if(matcher.matches(fileName)){
            final File visitedFile = file.toFile();
            files.add(visitedFile);
          }

          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException ignored){}

  }
}
