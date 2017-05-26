package com.faforever.client.remote.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.net.InetSocketAddress;

public class InetSocketAddressTypeAdapter extends TypeAdapter<InetSocketAddress> {

  public static final InetSocketAddressTypeAdapter INSTANCE = new InetSocketAddressTypeAdapter();

  private InetSocketAddressTypeAdapter() {

  }

  @Override
  public void write(JsonWriter out, InetSocketAddress value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }
    out.beginArray()
        .value(value.getHostString())
        .value(value.getPort())
        .endArray();
  }

  @Override
  public InetSocketAddress read(JsonReader in) throws IOException {
    in.beginArray();
    String host = in.nextString();
    int port = in.nextInt();
    in.endArray();
    return new InetSocketAddress(host, port);
  }
}
