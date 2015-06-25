package com.faforever.client.legacy.io;

import com.jcraft.jzlib.JZlib;

import java.io.ByteArrayOutputStream;
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

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(bytes.length);

    try(DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(out, deflater, true)) {
      deflaterOutputStream.flush();
    }

    return out.toByteArray();
  }
}
