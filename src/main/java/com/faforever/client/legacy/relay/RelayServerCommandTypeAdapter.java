package com.faforever.client.legacy.relay;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class RelayServerCommandTypeAdapter extends TypeAdapter<RelayServerCommand>{

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void write(JsonWriter out, RelayServerCommand value) throws IOException {
    out.value(value.getString());
  }

  @Override
  public RelayServerCommand read(JsonReader in) throws IOException {
    return RelayServerCommand.fromString(in.nextString());
  }
}
