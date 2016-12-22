package com.faforever.client.remote.gson;

import com.faforever.client.remote.domain.GameStatus;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class GameStateTypeAdapter extends TypeAdapter<GameStatus> {

  public static final GameStateTypeAdapter INSTANCE = new GameStateTypeAdapter();

  private GameStateTypeAdapter() {

  }

  @Override
  public void write(JsonWriter out, GameStatus value) throws IOException {
    if (value == null) {
      out.value(GameStatus.UNKNOWN.getString());
    } else {
      out.value(value.getString());
    }
  }

  @Override
  public GameStatus read(JsonReader in) throws IOException {
    return GameStatus.fromString(in.nextString());
  }
}
