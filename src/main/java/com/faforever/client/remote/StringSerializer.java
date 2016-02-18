package com.faforever.client.remote;

import org.springframework.core.serializer.Serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class StringSerializer implements Serializer<String> {

  @Override
  public void serialize(String string, OutputStream outputStream) throws IOException {
    outputStream.write(string.getBytes(StandardCharsets.UTF_16));
  }
}
