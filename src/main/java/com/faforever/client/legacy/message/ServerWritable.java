package com.faforever.client.legacy.message;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public interface ServerWritable {

  void write(Gson gson, Writer writer) throws IOException;

  /**
   * Returns a list of strings that should be masked when logged (due to security reasons), e. g. a password.
   */
  List<String> getStringsToMask();
}
