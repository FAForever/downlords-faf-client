package com.faforever.client.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ByteCopier {

  private final InputStream inputStream;
  private ProgressListener progressListener;
  private int byteCountInterval;
  private int bufferSize;
  private long totalBytes;
  private OutputStream outputStream;
  private long lastCountUpdate;

  private ByteCopier(InputStream inputStream) {
    this.inputStream = inputStream;
    // 4K
    bufferSize = 0x1000;
    byteCountInterval = 40;
  }

  public static ByteCopier from(InputStream inputStream) {
    return new ByteCopier(inputStream);
  }

  public ByteCopier to(OutputStream outputStream) {
    this.outputStream = outputStream;
    return this;
  }

  /**
   * Sets the interval between in which listeners should receive updated byte counts, in milliseconds.
   */
  public ByteCopier byteCountInterval(int byteCountInterval) {
    this.byteCountInterval = byteCountInterval;
    return this;
  }

  public ByteCopier listener(ProgressListener progressListener) {
    this.progressListener = progressListener;
    return this;
  }

  public ByteCopier bufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
    return this;
  }

  public ByteCopier totalBytes(long totalBytes) {
    this.totalBytes = totalBytes;
    return this;
  }

  public void copy() throws IOException {
    byte[] buffer = new byte[bufferSize];

    int bytesDone = 0;

    int length;
    while ((length = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, length);
      bytesDone += length;

      long now = System.currentTimeMillis();
      if (progressListener != null && lastCountUpdate < now - byteCountInterval) {
        progressListener.update(bytesDone, totalBytes);
        lastCountUpdate = now;
      }
    }
  }
}
