package com.faforever.client.fa.relay;

import com.faforever.client.remote.JsonMessageSerializer;
import com.faforever.client.remote.domain.MessageTarget;
import com.faforever.client.remote.gson.GpgClientMessageTypeAdapter;
import com.faforever.client.remote.gson.GpgServerMessageTypeTypeAdapter;
import com.faforever.client.remote.gson.MessageTargetTypeAdapter;
import com.google.gson.GsonBuilder;

public class GpgClientMessageSerializer extends JsonMessageSerializer<GpgGameMessage> {

  @Override
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    gsonBuilder.registerTypeAdapter(GpgClientCommand.class, GpgClientMessageTypeAdapter.INSTANCE);
    gsonBuilder.registerTypeAdapter(GpgServerMessageType.class, GpgServerMessageTypeTypeAdapter.INSTANCE);
    gsonBuilder.registerTypeAdapter(MessageTarget.class, MessageTargetTypeAdapter.INSTANCE);
  }
}
