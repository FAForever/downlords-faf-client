package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.domain.StatisticsType;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class StatisticsTypeTypeAdapter extends TypeAdapter<StatisticsType> {

  @Override
  public void write(JsonWriter out, StatisticsType value) throws IOException {
    out.value(value.getString());
  }

  @Override
  public StatisticsType read(JsonReader in) throws IOException {
    return StatisticsType.fromString(in.nextString());
  }
}
