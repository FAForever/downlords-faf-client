package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.relay.GpgClientCommand;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class GpgClientCommandTypeAdapter extends TypeAdapter<GpgClientCommand> {

  public static final GpgClientCommandTypeAdapter INSTANCE = new GpgClientCommandTypeAdapter();

  private GpgClientCommandTypeAdapter() {

  }

  @Override
  public void write(JsonWriter out, GpgClientCommand value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      out.value(value.getString());
    }
  }

  @Override
  public GpgClientCommand read(JsonReader in) throws IOException {
    return GpgClientCommand.fromString(in.nextString());
  }
}
