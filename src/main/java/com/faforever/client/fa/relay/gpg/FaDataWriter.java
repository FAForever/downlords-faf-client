package com.faforever.client.fa.relay.gpg;

import com.google.common.io.LittleEndianDataOutputStream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Writes data to Forged Alliance (the forgedalliance, not the lobby).
 */
public class FaDataWriter implements AutoCloseable {

  public static final int FIELD_TYPE_INT = 0;
  public static final int FIELD_TYPE_STRING = 1;
  private final LittleEndianDataOutputStream outputStream;

  public FaDataWriter(OutputStream outputStream) {
    this.outputStream = new LittleEndianDataOutputStream(new BufferedOutputStream(outputStream));
  }

  private void writeArgs(List<Object> args) throws IOException {
    outputStream.writeInt(args.size());

    for (Object arg : args) {
      switch (arg) {
        case Double value -> {
          outputStream.writeByte(FIELD_TYPE_INT);
          outputStream.writeInt(value.intValue());
        }
        case Integer value -> {
          outputStream.writeByte(FIELD_TYPE_INT);
          outputStream.writeInt(value);
        }
        case String value -> {
          outputStream.writeByte(FIELD_TYPE_STRING);
          writeString(value);
        }
        default -> throw new IllegalArgumentException("Unsupported argument type: " + arg.getClass());
      }
    }
  }

  private void writeString(String string) throws IOException {
    outputStream.writeInt(string.length());
    outputStream.write(string.getBytes(StandardCharsets.UTF_8));
  }

  public void writeMessage(GPGMessage message) throws IOException {
    writeString(message.command());
    writeArgs(message.arguments());
    outputStream.flush();
  }

  @Override
  public void close() throws IOException {
    outputStream.close();
  }
}
