package com.faforever.client.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardOpenOption.CREATE;

public final class Unzipper {

  public interface ByteCountListener {

    void updateBytesWritten(long written, long total);
  }

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final ZipInputStream zipInputStream;
  private ByteCountListener byteCountListener;
  private int byteCountInterval;
  private int bufferSize;
  private long totalBytes;
  private Path targetDirectory;
  private long lastCountUpdate;

  private Unzipper(ZipInputStream zipInputStream) {
    this.zipInputStream = zipInputStream;
    // 4K
    bufferSize = 0x1000;
    byteCountInterval = 333;
  }

  public Unzipper to(Path targetDirectory) {
    this.targetDirectory = targetDirectory;
    return this;
  }

  public Unzipper byteCountInterval(int byteCountInterval) {
    this.byteCountInterval = byteCountInterval;
    return this;
  }

  public Unzipper listener(ByteCountListener byteCountListener) {
    this.byteCountListener = byteCountListener;
    return this;
  }

  public Unzipper bufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
    return this;
  }

  public Unzipper totalBytes(long totalBytes) {
    this.totalBytes = totalBytes;
    return this;
  }

  public void unzip() throws IOException {
    byte[] buffer = new byte[bufferSize];

    long bytesDone = 0;

    ZipEntry zipEntry;
    while ((zipEntry = zipInputStream.getNextEntry()) != null) {
      Path targetFile = targetDirectory.resolve(zipEntry.getName());
      if (zipEntry.isDirectory()) {
        logger.trace("Creating directory {}", targetFile);
        Files.createDirectories(targetFile);
        continue;
      }

      Path parentDirectory = targetFile.getParent();
      if (Files.notExists(parentDirectory)) {
        logger.trace("Creating directory {}", parentDirectory);
        Files.createDirectories(parentDirectory);
      }

      long compressedSize = zipEntry.getCompressedSize();
      if (compressedSize != -1) {
        bytesDone += compressedSize;
      }

      logger.trace("Writing file {}", targetFile);
      try (OutputStream outputStream = Files.newOutputStream(targetFile, CREATE)) {
        int length;
        while ((length = zipInputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, length);

          long now = System.currentTimeMillis();
          if (byteCountListener != null && lastCountUpdate < now - byteCountInterval) {
            byteCountListener.updateBytesWritten(bytesDone, totalBytes);
            lastCountUpdate = now;
          }
        }
      }
    }
  }

  public static Unzipper from(ZipInputStream zipInputStream) {
    return new Unzipper(zipInputStream);
  }
}
