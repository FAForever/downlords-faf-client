package com.faforever.client.legacy.gson;

import com.faforever.client.portcheck.ConnectivityState;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class ConnectivityStateTypeAdapter extends TypeAdapter<ConnectivityState> {

  public static final ConnectivityStateTypeAdapter INSTANCE = new ConnectivityStateTypeAdapter();

  private ConnectivityStateTypeAdapter() {

  }

  @Override
  public void write(JsonWriter out, ConnectivityState value) throws IOException {
    out.value(value.getString());
  }

  @Override
  public ConnectivityState read(JsonReader in) throws IOException {
    return ConnectivityState.fromString(in.nextString());
  }


}
