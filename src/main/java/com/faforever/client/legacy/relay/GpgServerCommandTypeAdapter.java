package com.faforever.client.legacy.relay;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class GpgServerCommandTypeAdapter extends TypeAdapter<GpgServerMessageType> {

  public static final GpgServerCommandTypeAdapter INSTANCE = new GpgServerCommandTypeAdapter();

  private GpgServerCommandTypeAdapter() {

  }

  @Override
  public void write(JsonWriter out, GpgServerMessageType value) throws IOException {
    out.value(value.getString());
  }

  @Override
  public GpgServerMessageType read(JsonReader in) throws IOException {
    return GpgServerMessageType.fromString(in.nextString());
  }
}
