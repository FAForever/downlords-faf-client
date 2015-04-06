package com.faforever.client.legacy.message;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Writer;

public interface ServerWritable {

  void write(Gson gson, Writer writer) throws IOException;

  boolean isConfidential();
}
