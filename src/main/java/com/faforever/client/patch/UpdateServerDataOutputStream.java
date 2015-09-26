package com.faforever.client.patch;

import com.google.common.io.LittleEndianDataOutputStream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class UpdateServerDataOutputStream extends OutputStream {

  private final LittleEndianDataOutputStream outputStream;
  private Charset charset;

  public UpdateServerDataOutputStream(OutputStream outputStream) {
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

  public void writeString(String string) throws IOException {
    outputStream.write(string.getBytes(charset));
  }
}
