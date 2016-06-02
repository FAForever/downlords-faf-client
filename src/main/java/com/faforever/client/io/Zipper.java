package com.faforever.client.io;

import com.faforever.client.util.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class Zipper {

  public interface ByteCountListener {

    void updateBytesWritten(long written, long total);
  }

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Path directoryToZip;
  private boolean zipContent;
  private ByteCountListener byteCountListener;
  private int byteCountInterval;
  private int bufferSize;
  private long bytesTotal;
  private long bytesDone;
  private ZipOutputStream zipOutputStream;
  private long lastCountUpdate;
  private byte[] buffer;

  /**
   * @param zipContent {@code true} if the contents of the directory should be zipped, {@code false} if the specified
   * file (directory) should be zipped directly.
   */
  private Zipper(Path directoryToZip, boolean zipContent) {
    this.directoryToZip = directoryToZip;
    this.zipContent = zipContent;
    // 4K
    bufferSize = 0x1000;
    byteCountInterval = 40;
  }

  public Zipper to(ZipOutputStream zipOutputStream) {
    this.zipOutputStream = zipOutputStream;
    return this;
  }

  public Zipper byteCountInterval(int byteCountInterval) {
    this.byteCountInterval = byteCountInterval;
    return this;
  }

  public Zipper listener(ByteCountListener byteCountListener) {
    this.byteCountListener = byteCountListener;
    return this;
  }

  public void zip() throws IOException {
    Validator.notNull(zipOutputStream, "zipOutputStream must not be null");
    Validator.notNull(directoryToZip, "directoryToZip must not be null");

    bytesTotal = calculateTotalBytes();
    bytesDone = 0;
    buffer = new byte[bufferSize];

    Files.walkFileTree(directoryToZip, new SimpleFileVisitor<Path>() {
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (zipContent) {
          zipOutputStream.putNextEntry(new ZipEntry(directoryToZip.relativize(dir).toString() + "/"));
        } else {
          zipOutputStream.putNextEntry(new ZipEntry(directoryToZip.getParent().relativize(dir).toString() + "/"));
        }

        zipOutputStream.closeEntry();
        return FileVisitResult.CONTINUE;
      }

      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        logger.trace("Zipping file {}", file.toAbsolutePath());

        if (zipContent) {
          zipOutputStream.putNextEntry(new ZipEntry(directoryToZip.relativize(file).toString()));
        } else {
          zipOutputStream.putNextEntry(new ZipEntry(directoryToZip.getParent().relativize(file).toString()));
        }

        try (InputStream inputStream = Files.newInputStream(file)) {
          copy(inputStream, zipOutputStream);
        }
        zipOutputStream.closeEntry();
        return FileVisitResult.CONTINUE;
      }
    });
  }

  private long calculateTotalBytes() throws IOException {
    final long[] bytesTotal = {0};
    Files.walkFileTree(directoryToZip, new SimpleFileVisitor<Path>() {
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        bytesTotal[0] += Files.size(file);
        return FileVisitResult.CONTINUE;
      }
    });
    return bytesTotal[0];
  }

  private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
    int length;
    while ((length = inputStream.read(buffer)) > 0) {
      outputStream.write(buffer, 0, length);
      bytesDone += length;

      long now = System.currentTimeMillis();
      if (byteCountListener != null && lastCountUpdate < now - byteCountInterval) {
        byteCountListener.updateBytesWritten(bytesDone, bytesTotal);
        lastCountUpdate = now;
      }
    }
  }

  public static Zipper of(Path path) {
    return new Zipper(path, false);
  }

  public static Zipper contentOf(Path path) {
    return new Zipper(path, true);
  }
}
