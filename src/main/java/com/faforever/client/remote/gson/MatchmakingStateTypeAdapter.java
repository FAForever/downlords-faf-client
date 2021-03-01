package com.faforever.client.remote.gson;

import com.faforever.client.remote.domain.MatchmakingState;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class MatchmakingStateTypeAdapter extends TypeAdapter<MatchmakingState> {

  public static final MatchmakingStateTypeAdapter INSTANCE = new MatchmakingStateTypeAdapter();

  private MatchmakingStateTypeAdapter() {
    // private
  }

  @Override
  public void write(JsonWriter out, MatchmakingState value) throws IOException {
    out.value(value.getString());
  }

  @Override
  public MatchmakingState read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }
    return MatchmakingState.fromString(in.nextString());
  }
}
