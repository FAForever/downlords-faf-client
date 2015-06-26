package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.domain.VictoryCondition;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class VictoryConditionTypeAdapter extends TypeAdapter<VictoryCondition> {

  @Override
  public void write(JsonWriter out, VictoryCondition value) throws IOException {
    if (value == null) {
      out.value("unknown");
    } else {
      out.value(value.getNumber());
    }
  }

  @Override
  public VictoryCondition read(JsonReader in) throws IOException {
    String gameType = in.nextString();
    if ("unknown".equals(gameType)) {
      return VictoryCondition.UNKNOWN;
    }
    return VictoryCondition.fromNumber(Integer.valueOf(gameType));
  }
}
