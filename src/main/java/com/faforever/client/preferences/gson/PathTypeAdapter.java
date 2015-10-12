package com.faforever.client.preferences.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathTypeAdapter extends TypeAdapter<Path> {

  public static final PathTypeAdapter INSTANCE = new PathTypeAdapter();

  private PathTypeAdapter() {
  }

  @Override
  public void write(JsonWriter out, Path value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      out.value(value.toAbsolutePath().toString());
    }
  }

  @Override
  public Path read(JsonReader in) throws IOException {
    String string = in.nextString();
    if (string == null) {
      return null;
    } else {
      return Paths.get(string);
    }
  }
}
