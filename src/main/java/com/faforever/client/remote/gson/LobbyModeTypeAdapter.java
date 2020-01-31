package com.faforever.client.remote.gson;

import com.faforever.client.fa.relay.LobbyMode;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class LobbyModeTypeAdapter extends TypeAdapter<LobbyMode> {

  public static final LobbyModeTypeAdapter INSTANCE = new LobbyModeTypeAdapter();

  private LobbyModeTypeAdapter() {
    // private
  }

  @Override
  public void write(JsonWriter out, LobbyMode value) throws IOException {
    out.value(value.ordinal());
  }

  @Override
  public LobbyMode read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }
    return LobbyMode.values()[in.nextInt()];
  }
}
