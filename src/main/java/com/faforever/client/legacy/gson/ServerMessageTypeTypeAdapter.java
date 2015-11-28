package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.domain.FafServerMessageType;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class ServerMessageTypeTypeAdapter extends TypeAdapter<FafServerMessageType> {

  public static final ServerMessageTypeTypeAdapter INSTANCE = new ServerMessageTypeTypeAdapter();

  private ServerMessageTypeTypeAdapter() {

  }

  @Override
  public void write(JsonWriter out, FafServerMessageType value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      out.value(value.getString());
    }
  }

  @Override
  public FafServerMessageType read(JsonReader in) throws IOException {
    return FafServerMessageType.fromString(in.nextString());
  }
}
