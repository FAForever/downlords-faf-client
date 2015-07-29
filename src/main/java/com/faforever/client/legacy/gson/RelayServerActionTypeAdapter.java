package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.relay.LobbyAction;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class RelayServerActionTypeAdapter extends TypeAdapter<LobbyAction> {

  @Override
  public void write(JsonWriter out, LobbyAction value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      out.value(value.getString());
    }
  }

  @Override
  public LobbyAction read(JsonReader in) throws IOException {
    return LobbyAction.fromString(in.nextString());
  }
}
