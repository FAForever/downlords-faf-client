package com.faforever.client.legacy.message;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

public enum PongMessage implements ServerWritable {
  INSTANCE;

  @Override
  public void write(Gson gson, Writer writer) throws IOException {
    writer.append("PONG");
  }

  @Override
  public List<String> getStringsToMask() {
    return Collections.emptyList();
  }
}
