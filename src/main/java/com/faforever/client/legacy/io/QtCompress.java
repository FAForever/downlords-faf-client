package com.faforever.client.legacy.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

public final class QtCompress {

  private QtCompress() {
    throw new AssertionError("Not instantiatable");
  }

  public static byte[] qUncompress(byte[] bytes) throws IOException {
    Inflater inflater = new Inflater();
    inflater.setInput(Arrays.copyOfRange(bytes, 4, bytes.length));

    ByteArrayOutputStream byteArray = new ByteArrayOutputStream();

    try (InflaterOutputStream inflaterOutputStream = new InflaterOutputStream(byteArray, inflater)) {
      inflaterOutputStream.flush();
    }

    return byteArray.toByteArray();
  }

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
