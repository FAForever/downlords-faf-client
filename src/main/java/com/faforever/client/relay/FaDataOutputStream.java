package com.faforever.client.relay;

import com.google.common.io.LittleEndianDataOutputStream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Writes data to Forged Alliance (the game, not the lobby).
 */
public class FaDataOutputStream extends OutputStream {

  public static final int FIELD_TYPE_INT = 0;
  public static final int FIELD_TYPE_FOLLOWING_STRING = 2;
  public static final int FIELD_TYPE_STRING = 1;
  public static final char DELIMITER = '\b';
  private final LittleEndianDataOutputStream outputStream;
  private Charset charset;

  public FaDataOutputStream(OutputStream outputStream) {
    this.outputStream = new LittleEndianDataOutputStream(new BufferedOutputStream(outputStream));
    charset = StandardCharsets.UTF_8;
  }

  @Override
  public void write(int b) throws IOException {
    outputStream.write(b);
  }

  @Override
  public void flush() throws IOException {
    outputStream.flush();
  }

  public void writeArgs(List<Object> args) throws IOException {
    writeInt(args.size());

    for (Object arg : args) {
      if (arg instanceof Double) {
        writeByte(FIELD_TYPE_INT);
        writeInt(((Double) arg).intValue());
      } else if (arg instanceof Integer) {
        writeByte(FIELD_TYPE_INT);
        writeInt((int) arg);
      } else if (arg instanceof String) {
        String value = (String) arg;
        writeByte(FIELD_TYPE_STRING);
        writeInt(value.length());
        writeString(value);
      }
    }
  }

  public void writeInt(int value) throws IOException {
    outputStream.writeInt(value);
  }

  public void writeByte(int b) throws IOException {
    outputStream.writeByte(b);
  }

  public void writeString(String string) throws IOException {
    outputStream.write(string.getBytes(charset));
  }
}
