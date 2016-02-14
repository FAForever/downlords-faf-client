package com.faforever.client.theme;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.function.Consumer;

public class DirectoryVisitor extends SimpleFileVisitor<Path> {

  private Consumer<Path> onDirectoryFoundListener;

  public DirectoryVisitor(Consumer<Path> onDirectoryFoundListener) {
    this.onDirectoryFoundListener = onDirectoryFoundListener;
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
    if (exc != null) {
      throw exc;
    }

    onDirectoryFoundListener.accept(dir);
    return FileVisitResult.CONTINUE;
  }
}
