package com.faforever.client.remote.gson;

import com.faforever.client.fa.relay.GpgClientCommand;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class GpgClientMessageTypeAdapter extends TypeAdapter<GpgClientCommand> {

  public static final GpgClientMessageTypeAdapter INSTANCE = new GpgClientMessageTypeAdapter();

  private GpgClientMessageTypeAdapter() {

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
