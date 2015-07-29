package com.faforever.client.legacy.relay;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

// TODO turn all type adapters into singleton
public class RelayServerCommandTypeAdapter extends TypeAdapter<RelayServerCommand>{

  @Override
  public void write(JsonWriter out, RelayServerCommand value) throws IOException {
    out.value(value.getString());
  }

  @Override
  public RelayServerCommand read(JsonReader in) throws IOException {
    return RelayServerCommand.fromString(in.nextString());
  }
}
