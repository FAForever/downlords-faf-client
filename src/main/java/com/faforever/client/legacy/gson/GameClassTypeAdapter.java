package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.domain.GameAccess;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class GameClassTypeAdapter extends TypeAdapter<GameAccess> {

  @Override
  public void write(JsonWriter out, GameAccess value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      out.value(value.getString());
    }
  }

  @Override
  public GameAccess read(JsonReader in) throws IOException {
    return GameAccess.valueOf(in.nextString());
  }
}
