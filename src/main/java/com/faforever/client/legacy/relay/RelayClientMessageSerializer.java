package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.gson.RelayServerActionTypeAdapter;
import com.faforever.client.legacy.writer.JsonMessageSerializer;
import com.google.gson.GsonBuilder;

public class RelayClientMessageSerializer extends JsonMessageSerializer<LobbyMessage> {

  @Override
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    gsonBuilder.registerTypeAdapter(LobbyAction.class, RelayServerActionTypeAdapter.INSTANCE);
    gsonBuilder.registerTypeAdapter(RelayServerCommand.class, RelayServerCommandTypeAdapter.INSTANCE);
  }
}
