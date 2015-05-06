package com.faforever.client.legacy.domain;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Implemented by classes that can be serialized in a JSON-like string that is readable by the FAF server.
 */
public interface ServerWritable {

  void write(Gson gson, Writer writer) throws IOException;

  /**
   * Returns a list of strings that should be masked when logged, e. g. a password.
   */
  List<String> getStringsToMask();
}
