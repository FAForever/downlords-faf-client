package com.faforever.client.remote.gson;

import com.faforever.client.relay.GpgServerMessageType;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class GpgServerMessageTypeTypeAdapter extends TypeAdapter<GpgServerMessageType> {

  public static final GpgServerMessageTypeTypeAdapter INSTANCE = new GpgServerMessageTypeTypeAdapter();

  private GpgServerMessageTypeTypeAdapter() {

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
