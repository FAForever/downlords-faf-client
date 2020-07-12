package com.faforever.client.io;

import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.file.Files.walkFileTree;

public final class FileUtils {

  private FileUtils() {
    throw new AssertionError("Not instantiatable");
  }

  public static void deleteRecursively(Path path) throws IOException {
    if (Files.notExists(path)) {
      return;
    }
    walkFileTree(path, new SimpleFileVisitor<>() {
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
   * @param source the directory whose content should be copied
   * @param target the directory into which the content of the source directory will be copied
   */
  @SneakyThrows
  public static void copyContentRecursively(Path source, Path target) {
    try (Stream<Path> stream = Files.walk(source)) {
      stream.forEach(sourcePath -> noCatch(() -> {
        Path targetFile = target.resolve(source.relativize(sourcePath));
        if (Files.isDirectory(sourcePath)) {
          Files.createDirectories(targetFile);
        } else {
          Files.copy(sourcePath, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
      }));
    }
  }
}
