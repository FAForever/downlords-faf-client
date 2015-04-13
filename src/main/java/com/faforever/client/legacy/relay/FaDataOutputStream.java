package com.faforever.client.legacy.relay;

import com.google.common.io.LittleEndianDataOutputStream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

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

  public void writeInt(int value) throws IOException {
    outputStream.writeInt(value);
  }

  public void writeString(String string) throws IOException {
    outputStream.write(string.getBytes(charset));
  }

  public void writeByte(int b) throws IOException {
    outputStream.writeByte(b);
  }

  public void writeChunks(List<Object> chunks) throws IOException {
    writeInt(chunks.size());

    for (Object chunk : chunks) {
      if (chunk instanceof Double) {
        int value = ((Double) chunk).intValue();
        writeByte(FIELD_TYPE_INT);
        writeInt(value);
      } else {
        String value = (String) chunk;
        writeByte(FIELD_TYPE_STRING);
        writeInt(value.length());
        writeString(value);
      }
    }
  }

  public void writeUdpChunks(List<Object> chunks) throws IOException {
    writeInt(chunks.size());

    boolean isFollowingString = false;

    for (Object chunk : chunks) {
      if (chunk instanceof Double) {
        int value = ((Double) chunk).intValue();
        writeByte(FIELD_TYPE_INT);
        writeInt(value);
      } else {
        String value = ((String) chunk).replace("\t", "/t").replace("\n", "/n");

        if (isFollowingString) {
          value = DELIMITER + value;
          writeByte(FIELD_TYPE_FOLLOWING_STRING);
        } else {
          writeByte(FIELD_TYPE_STRING);
        }

        writeInt(value.length());
        writeString(value);
      }

      isFollowingString = true;
    }
  }

  @Override
  public void flush() throws IOException {
    outputStream.flush();
  }
}
