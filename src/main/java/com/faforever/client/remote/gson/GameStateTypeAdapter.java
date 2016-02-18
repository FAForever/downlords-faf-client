package com.faforever.client.remote.gson;

import com.faforever.client.remote.domain.GameState;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class GameStateTypeAdapter extends TypeAdapter<GameState> {

  public static final GameStateTypeAdapter INSTANCE = new GameStateTypeAdapter();

  private GameStateTypeAdapter() {

  }

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
