package com.faforever.client.legacy;

import java.io.Closeable;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class QDataOutputStream extends Writer {

  private DataOutput dataOutput;
  private Charset charset;

  public QDataOutputStream(DataOutput dataOutput) {
    this(dataOutput, StandardCharsets.UTF_16BE);
  }

  public QDataOutputStream(DataOutput dataOutput, Charset charset) {
    this.dataOutput = dataOutput;
    this.charset = charset;
  }

  public void writeQString(String value) throws IOException {
    dataOutput.writeChars(value);
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    dataOutput.write(new String(cbuf).getBytes(charset), off, len);
  }

  @Override
  public void flush() throws IOException {
    ((OutputStream) dataOutput).flush();
  }

  @Override
  public void close() throws IOException {
    if (dataOutput instanceof Closeable) {
      ((Closeable) dataOutput).close();
    }
  }

  public void writeQInt(int value) throws IOException {
    dataOutput.writeInt(value);
  }

  public void writeRaw(byte[] bytes) throws IOException {
    dataOutput.write(bytes);
  }

  public void writeBoolean(boolean value) throws IOException {
    dataOutput.writeBoolean(value);
  }
}
