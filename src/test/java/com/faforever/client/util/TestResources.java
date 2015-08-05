package com.faforever.client.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestResources {

  private TestResources() {
    throw new AssertionError("Not instantiatable");
  }

  /**
   * Copies the content of a classpath resource to a target file. The parent directories of the target file are created
   * automatically.
   */
  public static void copyResource(String classPathLocation, Path targetFile) throws IOException {
    Files.createDirectories(targetFile.getParent());
    try (InputStream inputStream = TestResources.class.getResourceAsStream(classPathLocation)) {
      Files.copy(inputStream, targetFile);
    }
  }
}
