package com.faforever.client.legacy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class QStreamWriter implements Appendable {

  private final OutputStream out;

  public QStreamWriter(OutputStream out) {
    this.out = out;
  }

  @Override
  public QStreamWriter append(CharSequence csq) throws IOException {
    if (csq == null) {
      append(new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,});
      return this;
    }

    byte[] bytes = csq.toString().getBytes(StandardCharsets.UTF_16BE);
    return append(bytes);
  }

  @Override
  public QStreamWriter append(CharSequence csq, int start, int end) throws IOException {
    throw new UnsupportedOperationException();
  }

  public QStreamWriter append(byte[] bytes) throws IOException {
    writeInt(bytes.length);
    out.write(bytes);
    return this;
  }

  @Override
  public Appendable append(char c) throws IOException {
    throw new UnsupportedOperationException();
  }

  public void flush() throws IOException {
    out.flush();
  }

  public final void writeInt(int v) throws IOException {
    out.write((v >>> 24) & 0xFF);
    out.write((v >>> 16) & 0xFF);
    out.write((v >>> 8) & 0xFF);
    out.write((v >>> 0) & 0xFF);
  }
}
