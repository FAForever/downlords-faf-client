package com.faforever.client.legacy.relay;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class RelayServerCommandTypeAdapter extends TypeAdapter<RelayServerCommand> {

  public static final RelayServerCommandTypeAdapter INSTANCE = new RelayServerCommandTypeAdapter();

  private RelayServerCommandTypeAdapter() {

  }

  @Override
  public void write(JsonWriter out, RelayServerCommand value) throws IOException {
    out.value(value.getString());
  }

  @Override
  public RelayServerCommand read(JsonReader in) throws IOException {
    return RelayServerCommand.fromString(in.nextString());
  }
}
