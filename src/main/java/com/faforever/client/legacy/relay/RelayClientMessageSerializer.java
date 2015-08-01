package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.gson.RelayServerActionTypeAdapter;
import com.faforever.client.legacy.writer.JsonMessageSerializer;
import com.google.gson.GsonBuilder;

public class RelayClientMessageSerializer extends JsonMessageSerializer<LobbyMessage> {

  @Override
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    gsonBuilder.registerTypeAdapter(LobbyAction.class, new RelayServerActionTypeAdapter());
    gsonBuilder.registerTypeAdapter(RelayServerCommand.class, new RelayServerCommandTypeAdapter());
  }
}
