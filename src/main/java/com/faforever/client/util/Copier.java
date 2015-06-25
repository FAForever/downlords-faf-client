package com.faforever.client.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public final class Copier {

  public interface ByteCountListener {

    void updateBytesWritten(long written, long total);
  }

  private ReadableByteChannel readableByteChannel;
  private WritableByteChannel writableByteChannel;
  private ByteCountListener byteCountListener;
  private int byteCountInterval;
  private int bufferSize;
  private long totalBytes;
  private long lastCountUpdate;

  private Copier(ReadableByteChannel readableByteChannel) {
    this.readableByteChannel = readableByteChannel;
    // 4K
    bufferSize = 0x1000;
    byteCountInterval = 333;
  }

  public static Copier from(ReadableByteChannel readableByteChannel) {
    return new Copier(readableByteChannel);
  }

  public Copier to(WritableByteChannel writableByteChannel) {
    this.writableByteChannel = writableByteChannel;
    return this;
  }

  public Copier byteCountInterval(int byteCountInterval) {
    this.byteCountInterval = byteCountInterval;
    return this;
  }

  public Copier listener(ByteCountListener byteCountListener) {
    this.byteCountListener = byteCountListener;
    return this;
  }

  public Copier bufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
    return this;
  }

  public Copier totalBytes(int totalBytes) {
    this.totalBytes = totalBytes;
    return this;
  }

  public void copy() throws IOException {
    ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

    int bytesDone = 0;

    while (readableByteChannel.read(buffer) != -1) {
      buffer.flip();
      bytesDone += writableByteChannel.write(buffer);
      buffer.compact();

      long now = System.currentTimeMillis();
      if (byteCountListener != null && lastCountUpdate < now - byteCountInterval) {
        byteCountListener.updateBytesWritten(bytesDone, totalBytes);
        lastCountUpdate = now;
      }
    }

    buffer.flip();
    while (buffer.hasRemaining()) {
      bytesDone += writableByteChannel.write(buffer);
      if (byteCountListener != null) {
        byteCountListener.updateBytesWritten(bytesDone, totalBytes);
      }
    }
  }
}
