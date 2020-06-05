package com.faforever.client.remote.gson;

import com.faforever.client.game.Faction;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.IOException;

public class ComparableVersionTypeAdapter extends TypeAdapter<ComparableVersion> {

  public static final ComparableVersionTypeAdapter INSTANCE = new ComparableVersionTypeAdapter();

  private ComparableVersionTypeAdapter() {
    // private
  }

  @Override
  public void write(JsonWriter out, ComparableVersion value) throws IOException {
    out.value(value.toString());
  }

  @Override
  public ComparableVersion read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }
    String version = in.nextString();
    version = version.startsWith("v") ? version.substring(1) : version;
    return new ComparableVersion(version);
  }
}
