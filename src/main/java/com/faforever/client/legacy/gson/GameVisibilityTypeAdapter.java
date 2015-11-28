package com.faforever.client.legacy.gson;

import com.faforever.client.game.GameVisibility;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class GameVisibilityTypeAdapter extends TypeAdapter<GameVisibility> {

  public static final GameVisibilityTypeAdapter INSTANCE = new GameVisibilityTypeAdapter();

  private GameVisibilityTypeAdapter() {

  }

  @Override
  public void write(JsonWriter out, GameVisibility value) throws IOException {
    out.value(value.getString());
  }

  @Override
  public GameVisibility read(JsonReader in) throws IOException {
    return GameVisibility.fromString(in.nextString());
  }
}
