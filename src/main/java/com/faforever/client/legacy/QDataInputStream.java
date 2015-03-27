package com.faforever.client.legacy;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class QDataInputStream extends Reader {

  private DataInputStream inputStream;

  public QDataInputStream(DataInputStream inputStream) {
    this.inputStream = inputStream;
  }

  public String readQString() throws IOException {
    int stringSize = inputStream.readInt();
    byte[] buffer = new byte[stringSize];
    inputStream.readFully(buffer);
    return new String(buffer, StandardCharsets.UTF_16BE);
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() throws IOException {
    inputStream.close();
  }

  public int readQInt() throws IOException {
    return inputStream.readInt();
  }

  /**
   * Skip the "block size" bytes, since we just don't care.
   */
  public void skipBlockSize() throws IOException {
    inputStream.skipBytes(Integer.SIZE / Byte.SIZE);
  }
}
