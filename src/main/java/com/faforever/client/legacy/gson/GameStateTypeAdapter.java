package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.domain.GameState;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class GameStateTypeAdapter extends TypeAdapter<GameState> {

  @Override
  public void write(JsonWriter out, GameState value) throws IOException {
    if (value == null) {
      out.value(GameState.UNKNOWN.getString());
    } else {
      out.value(value.getString());
    }
  }

  @Override
  public GameState read(JsonReader in) throws IOException {
    return GameState.fromString(in.nextString());
  }
}
