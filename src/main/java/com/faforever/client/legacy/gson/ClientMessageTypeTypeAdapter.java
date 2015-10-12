package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.domain.ClientMessageType;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class ClientMessageTypeTypeAdapter extends TypeAdapter<ClientMessageType> {

  public static final ClientMessageTypeTypeAdapter INSTANCE = new ClientMessageTypeTypeAdapter();

  private ClientMessageTypeTypeAdapter() {

  }

  @Override
  public void write(JsonWriter out, ClientMessageType value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      out.value(value.getString());
    }
  }

  @Override
  public ClientMessageType read(JsonReader in) throws IOException {
    return ClientMessageType.fromString(in.nextString());
  }
}
