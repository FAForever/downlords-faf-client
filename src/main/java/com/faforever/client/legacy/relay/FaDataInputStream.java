package com.faforever.client.legacy.relay;

import com.google.common.io.LittleEndianDataInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FaDataInputStream extends InputStream {

  private static final int MAX_CHUNK_SIZE = 100;

  private final LittleEndianDataInputStream inputStream;
  private Charset charset;

  public FaDataInputStream(InputStream inputStream) {
    this.inputStream = new LittleEndianDataInputStream(new BufferedInputStream(inputStream));
    charset = StandardCharsets.UTF_8;
  }


  @Override
  public int read() throws IOException {
    return inputStream.read();
  }

  public int readInt() throws IOException {
    return inputStream.readInt();
  }

  public String readString() throws IOException {
    int size = readInt();

    // FIXME out of memory
    byte[] buffer = new byte[size];
    inputStream.readFully(buffer);
    return new String(buffer, charset);
  }

  public boolean readBoolean() throws IOException {
    return inputStream.readBoolean();
  }

  public List<Object> readChunks() throws IOException {
    int numberOfChunks = readInt();

    if (numberOfChunks > MAX_CHUNK_SIZE) {
      throw new IOException("Too many chunks: " + numberOfChunks);
    }

    List<Object> chunks = new ArrayList<>(numberOfChunks);

    for (int chunkNumber = 0; chunkNumber < numberOfChunks; chunkNumber++) {
      boolean isBoolean = readBoolean();
      String dataString = readString();

      // This could surely be optimized
      String fixedString = dataString.replace("/t", "\t").replace("/n", "\n");

      chunks.add(fixedString);
    }

    return chunks;
  }
}
