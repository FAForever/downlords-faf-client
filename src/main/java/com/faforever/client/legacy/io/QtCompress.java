package com.faforever.client.legacy.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public final class QtCompress {

  private QtCompress() {
    // Utility class
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
