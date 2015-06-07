package com.faforever.client.legacy.writer;

import java.io.Closeable;
import java.io.DataInput;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class QDataInputStream extends Reader {

  private DataInput dataInput;
  private Charset charset;

  public QDataInputStream(DataInput dataInput) {
    this(dataInput, StandardCharsets.UTF_16BE);
  }

  public QDataInputStream(DataInput dataInput, Charset charset) {
    this.dataInput = dataInput;
    this.charset = charset;
  }

  public String readQString() throws IOException {
    int stringSize = dataInput.readInt();
    byte[] buffer = new byte[stringSize];
    dataInput.readFully(buffer);
    return new String(buffer, charset);
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() throws IOException {
    if (dataInput instanceof Closeable) {
      ((Closeable) dataInput).close();
    }
  }

  public int readInt32() throws IOException {
    return dataInput.readInt();
  }

  /**
   * Skip the "block size" bytes, since we just don't care.
   */
  public void skipBlockSize() throws IOException {
    dataInput.skipBytes(Integer.SIZE / Byte.SIZE);
  }

  public String readRaw(int size) throws IOException {
    byte[] buffer = new byte[size];
    dataInput.readFully(buffer);
    return new String(buffer, charset);
  }

  public boolean readBoolean() throws IOException {
    return dataInput.readBoolean();
  }
}
