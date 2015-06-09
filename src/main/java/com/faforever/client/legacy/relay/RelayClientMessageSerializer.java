package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.gson.RelayServerActionTypeAdapter;
import com.faforever.client.legacy.writer.JsonSerializer;
import com.google.gson.GsonBuilder;

public class RelayClientMessageSerializer extends JsonSerializer<RelayClientMessage> {

  @Override
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    gsonBuilder.registerTypeAdapter(RelayServerAction.class, new RelayServerActionTypeAdapter());
    gsonBuilder.registerTypeAdapter(RelayServerCommand.class, new RelayServerCommandTypeAdapter());
  }
}
