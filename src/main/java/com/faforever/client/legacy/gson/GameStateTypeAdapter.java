package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.GameState;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class GameStateTypeAdapter extends TypeAdapter<GameState> {

  @Override
  public void write(JsonWriter out, GameState value) throws IOException {
    if (value == null) {
      out.value("unknown");
    } else {
      out.value(value.getString());
    }
  }

  @Override
  public GameState read(JsonReader in) throws IOException {
    String gameState = in.nextString();
    if ("unknown".equals(gameState)) {
      return GameState.UNKNOWN;
    }
    return GameState.fromString(gameState);
  }
}
