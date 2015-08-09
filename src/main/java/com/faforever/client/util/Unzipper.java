package com.faforever.client.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardOpenOption.CREATE;

public final class Unzipper {

  public interface ByteCountListener {

    void updateBytesWritten(long written, long total);
  }

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

  public static Unzipper from(ZipInputStream zipInputStream) {
    return new Unzipper(zipInputStream);
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

  public Unzipper totalBytes(int totalBytes) {
    this.totalBytes = totalBytes;
    return this;
  }

  public void unzip() throws IOException {
    byte[] buffer = new byte[bufferSize];

    int bytesDone = 0;

    ZipEntry zipEntry;
    while ((zipEntry = zipInputStream.getNextEntry()) != null) {
      if (zipEntry.isDirectory()) {
        Files.createDirectories(targetDirectory.resolve(zipEntry.getName()));
        continue;
      }

      Path targetFile = targetDirectory.resolve(zipEntry.getName());

      if (Files.notExists(targetFile.getParent())) {
        Files.createDirectories(targetFile.getParent());
      }

      try (OutputStream outputStream = Files.newOutputStream(targetFile, CREATE)) {
        int length;
        while ((length = zipInputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, length);
          bytesDone += zipEntry.getCompressedSize();

          long now = System.currentTimeMillis();
          if (byteCountListener != null && lastCountUpdate < now - byteCountInterval) {
            byteCountListener.updateBytesWritten(bytesDone, totalBytes);
            lastCountUpdate = now;
          }
        }
      }
    }
  }
}
