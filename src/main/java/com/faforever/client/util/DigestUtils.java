package com.faforever.client.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DigestUtils {

  private DigestUtils() {
    // Utility class
  }

  public static String md5(Path path) throws IOException {
    if (!Files.isRegularFile(path)) {
      throw new IllegalArgumentException("Path is not a regular file: " + path);
    }

    try (InputStream inputStream = Files.newInputStream(path)) {
      return org.apache.commons.codec.digest.DigestUtils.md5Hex(inputStream);
    }
  }
}
