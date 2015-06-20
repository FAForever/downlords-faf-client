package com.faforever.client.legacy.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class QStreamWriter extends Writer {

  public static final Charset CHARSET = StandardCharsets.UTF_16BE;
  private OutputStream out;

  public QStreamWriter(OutputStream out) {
    this.out = out;
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    out.write(new String(cbuf).substring(off, len).getBytes(CHARSET));
  }

  @Override
  public Writer append(CharSequence csq) throws IOException {
    if (csq == null) {
      appendWithSize(new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,});
      return this;
    }

    byte[] bytes = csq.toString().getBytes(CHARSET);
    return appendWithSize(bytes);
  }

  @Override
  public void flush() throws IOException {
    out.flush();
  }

  @Override
  public void close() throws IOException {
    out.close();
  }

  /**
   * Appends the size of the given byte array to the stream followed by the byte array itself.
   */
  public QStreamWriter appendWithSize(byte[] bytes) throws IOException {
    writeInt32(bytes.length);
    out.write(bytes);
    return this;
  }

  public final void writeInt32(int v) throws IOException {
    out.write((v >>> 24) & 0xFF);
    out.write((v >>> 16) & 0xFF);
    out.write((v >>> 8) & 0xFF);
    out.write(v & 0xFF);
  }
}
