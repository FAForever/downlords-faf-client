package com.faforever.client.remote.gson;

import com.faforever.client.remote.domain.RatingRange;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class RatingRangeTypeAdapter extends TypeAdapter<RatingRange> {

  public static final RatingRangeTypeAdapter INSTANCE = new RatingRangeTypeAdapter();

  private RatingRangeTypeAdapter() {

  }

  @Override
  public void write(JsonWriter out, RatingRange value) throws IOException {
    out.beginArray();
    out.value(value.getMin());
    out.value(value.getMax());
    out.endArray();
  }

  @Override
  public RatingRange read(JsonReader in) throws IOException {
    in.beginArray();
    int min = in.nextInt();
    int max = in.nextInt();
    in.endArray();
    return new RatingRange(min, max);
  }
}
