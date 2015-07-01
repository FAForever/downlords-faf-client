package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.domain.VictoryCondition;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class VictoryConditionTypeAdapter extends TypeAdapter<VictoryCondition> {

  @Override
  public void write(JsonWriter out, VictoryCondition victoryCondition) throws IOException {
    if (victoryCondition == null) {
      out.value("unknown");
    } else {
      Object value = victoryCondition.getValue();
      if (value instanceof Integer) {
        out.value((int) value);
      } else {
        out.value((String) value);
      }
    }
  }

  @Override
  public VictoryCondition read(JsonReader in) throws IOException {
    String victoryCondition = in.nextString();
    if (victoryCondition == null || "unknown".equals(victoryCondition)) {
      return VictoryCondition.UNKNOWN;
    }
    return VictoryCondition.fromNumber(Integer.valueOf(victoryCondition));
  }
}
