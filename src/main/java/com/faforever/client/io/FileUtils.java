package com.faforever.client.io;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.file.Files.*;

public final class FileUtils {

  private FileUtils() {
    throw new AssertionError("Not instantiatable");
  }

  public static void deleteRecursively(Path path) throws IOException {
    walkFileTree(path, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
        if (e == null) {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        } else {
          throw e;
        }
      }
    });
  }

  /**
   * @param target the directory <strong>into</strong> which the source directory will be moved
   */
  public static void moveDirectoryInto(Path source, Path target) {
    noCatch(() -> walkFileTree(source, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        createDirectories(target.resolve(source.getParent().relativize(dir)));
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        move(file, target.resolve(source.getParent().relativize(file)));
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        delete(dir);
        return FileVisitResult.CONTINUE;
      }
    }));
  }
}
