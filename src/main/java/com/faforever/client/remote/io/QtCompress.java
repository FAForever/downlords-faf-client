package com.faforever.client.remote.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

/**
 * Utility class that compresses and uncompresses bytes like QT's <a href="http://doc.qt.io/qt-5/qbytearray.html">QByteArray</a>.
 */
public final class QtCompress {

  private QtCompress() {
    throw new AssertionError("Not instantiatable");
  }

  /**
   * Compresses the specified bytes like <a href="http://doc.qt.io/qt-5/qbytearray.html#qCompress">QByteArray.qCompress()</a>
   * does.
   */
  public static byte[] qUncompress(byte[] bytes) throws IOException {
    Inflater inflater = new Inflater();
    inflater.setInput(Arrays.copyOfRange(bytes, 4, bytes.length));

    ByteArrayOutputStream byteArray = new ByteArrayOutputStream();

    try (InflaterOutputStream inflaterOutputStream = new InflaterOutputStream(byteArray, inflater)) {
      inflaterOutputStream.flush();
    }

    return byteArray.toByteArray();
  }

  /**
   * Uncompresses the specified bytes like <a href="http://doc.qt.io/qt-5/qbytearray.html#qCompress">QByteArray.qCompress()</a>
   * does.
   */
  public static byte[] qCompress(byte[] bytes) throws IOException {
    Deflater deflater = new Deflater();
    deflater.setInput(bytes);

    ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
    new DataOutputStream(byteArray).writeInt(bytes.length);

    try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArray, deflater, true)) {
      deflaterOutputStream.flush();
    }

    return byteArray.toByteArray();
  }
}
