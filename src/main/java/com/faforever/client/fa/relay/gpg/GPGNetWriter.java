package com.faforever.client.fa.relay.gpg;

import com.google.common.io.LittleEndianDataOutputStream;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Writes data to Forged Alliance (the forgedalliance, not the lobby).
 */
@Slf4j
public class GPGNetWriter {

  public static final int FIELD_TYPE_INT = 0;
  public static final int FIELD_TYPE_STRING = 1;
  private final LittleEndianDataOutputStream outputStream;
  private final ReentrantLock messageLock = new ReentrantLock();

  public GPGNetWriter(OutputStream outputStream) {
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

  public void writeMessage(GPGMessage message) throws IOException, InterruptedException {
    messageLock.lockInterruptibly();
    try {
      writeString(message.command());
      writeArgs(message.arguments());
      outputStream.flush();
    } finally {
      messageLock.unlock();
    }
  }
}
