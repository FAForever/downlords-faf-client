package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.writer.JsonMessageSerializer;
import com.google.gson.GsonBuilder;

public class RelayServerMessageSerializer extends JsonMessageSerializer<RelayServerMessage> {

  @Override
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    super.addTypeAdapters(gsonBuilder);

    gsonBuilder.registerTypeAdapter(RelayServerCommand.class, new RelayServerCommandTypeAdapter());
  }
}
