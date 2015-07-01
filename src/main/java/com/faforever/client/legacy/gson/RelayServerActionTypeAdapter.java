package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.relay.RelayServerAction;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class RelayServerActionTypeAdapter extends TypeAdapter<RelayServerAction> {

  @Override
  public void write(JsonWriter out, RelayServerAction value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      out.value(value.getString());
    }
  }

  @Override
  public RelayServerAction read(JsonReader in) throws IOException {
    return RelayServerAction.fromString(in.nextString());
  }
}
