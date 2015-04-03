package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.GameStatus;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class GameStateTypeAdapter extends TypeAdapter<GameStatus> {

  @Override
  public void write(JsonWriter out, GameStatus value) throws IOException {
    if (value == null) {
      out.value("unknown");
    } else {
      out.value(value.getString());
    }
  }

  @Override
  public GameStatus read(JsonReader in) throws IOException {
    String gameState = in.nextString();
    if ("unknown".equals(gameState)) {
      return GameStatus.UNKNOWN;
    }
    return GameStatus.fromString(gameState);
  }
}
