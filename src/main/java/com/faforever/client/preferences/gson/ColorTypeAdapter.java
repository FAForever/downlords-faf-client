package com.faforever.client.preferences.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import javafx.scene.paint.Color;

import java.io.IOException;

public class ColorTypeAdapter extends TypeAdapter<Color> {

  @Override
  public void write(JsonWriter out, Color value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      out.value(value.toString());
    }
  }

  @Override
  public Color read(JsonReader in) throws IOException {
    return Color.web(in.nextString());
  }
}
