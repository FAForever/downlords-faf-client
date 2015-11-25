package com.faforever.client.legacy.relay;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class GpgServerCommandTypeAdapter extends TypeAdapter<GpgServerCommandServerCommand> {

  public static final GpgServerCommandTypeAdapter INSTANCE = new GpgServerCommandTypeAdapter();

  private GpgServerCommandTypeAdapter() {

  }

  @Override
  public void write(JsonWriter out, GpgServerCommandServerCommand value) throws IOException {
    out.value(value.getString());
  }

  @Override
  public GpgServerCommandServerCommand read(JsonReader in) throws IOException {
    return GpgServerCommandServerCommand.fromString(in.nextString());
  }
}
