package com.faforever.client.io;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Concrete implementation of {@link FileSystemResource} that counts the number of written bytes.
 */
public class CountingFileSystemResource extends FileSystemResource {

  private ProgressListener listener;

  public CountingFileSystemResource(Path file, ProgressListener listener) {
    super(file.toFile());
    this.listener = Optional.ofNullable(listener)
        .orElseThrow(() -> new IllegalArgumentException("'listener' must not be null"));
  }

  @Override
  @SneakyThrows
  public InputStream getInputStream() {
    return new CountingInputStream(getFile(), listener);
  }

  private class CountingInputStream extends FileInputStream {

    private final ProgressListener listener;
    private final long totalBytes;
    private long bytesDone;

    CountingInputStream(File file, ProgressListener listener) throws FileNotFoundException {
      super(file);
      this.listener = listener;
      this.totalBytes = file.length();
    }

    @Override
    public int read(@NotNull byte[] buffer) throws IOException {
      int bytesRead = super.read(buffer);
      if (bytesRead != -1) {
        this.bytesDone += bytesRead;
      }

      this.listener.update(this.bytesDone, totalBytes);
      return bytesRead;
    }
  }
}
