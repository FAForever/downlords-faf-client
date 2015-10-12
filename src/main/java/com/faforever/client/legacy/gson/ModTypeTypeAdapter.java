package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.domain.SearchModMessage;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class ModTypeTypeAdapter extends TypeAdapter<SearchModMessage.ModType> {

  public static final ModTypeTypeAdapter INSTANCE = new ModTypeTypeAdapter();

  private ModTypeTypeAdapter() {

  }

  @Override
  public void write(JsonWriter out, SearchModMessage.ModType value) throws IOException {
    out.value(value.getCode());
  }

  @Override
  public SearchModMessage.ModType read(JsonReader in) throws IOException {
    return SearchModMessage.ModType.fromCode(in.nextInt());
  }
}
