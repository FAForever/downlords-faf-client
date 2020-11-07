package com.faforever.client.remote.gson;

import com.faforever.client.remote.domain.GameType;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public final class GameTypeTypeAdapter extends TypeAdapter<GameType> {

  public static final GameTypeTypeAdapter INSTANCE = new GameTypeTypeAdapter();

  private GameTypeTypeAdapter() {
    // private
  }

  @Override
  public void write(JsonWriter out, GameType value) throws IOException {
    if (value == null) {
      out.value(GameType.UNKNOWN.getString());
    } else {
      out.value(value.getString());
    }
  }

  @Override
  public GameType read(JsonReader in) throws IOException {
    return GameType.fromString(in.nextString());
  }
}
