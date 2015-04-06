package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.message.GameType;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class GameTypeTypeAdapter extends TypeAdapter<GameType> {

  @Override
  public void write(JsonWriter out, GameType value) throws IOException {
    if (value == null) {
      out.value("unknown");
    } else {
      out.value(value.getNumber());
    }
  }

  @Override
  public GameType read(JsonReader in) throws IOException {
    String gameType = in.nextString();
    if ("unknown".equals(gameType)) {
      return GameType.UNKNOWN;
    }
    return GameType.fromNumber(Integer.valueOf(gameType));
  }
}
