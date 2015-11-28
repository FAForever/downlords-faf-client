package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.gson.GpgClientCommandTypeAdapter;
import com.faforever.client.legacy.writer.JsonMessageSerializer;
import com.google.gson.GsonBuilder;

public class GpgClientMessageSerializer extends JsonMessageSerializer<GpgClientMessage> {

  @Override
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    gsonBuilder.registerTypeAdapter(GpgClientCommand.class, GpgClientCommandTypeAdapter.INSTANCE);
    gsonBuilder.registerTypeAdapter(GpgServerMessageType.class, GpgServerCommandTypeAdapter.INSTANCE);
  }
}
