package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.legacy.gson.GpgClientMessageTypeAdapter;
import com.faforever.client.legacy.gson.GpgServerMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.MessageTargetTypeAdapter;
import com.faforever.client.legacy.writer.JsonMessageSerializer;
import com.google.gson.GsonBuilder;

public class GpgClientMessageSerializer extends JsonMessageSerializer<GpgClientMessage> {

  @Override
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    gsonBuilder.registerTypeAdapter(GpgClientCommand.class, GpgClientMessageTypeAdapter.INSTANCE);
    gsonBuilder.registerTypeAdapter(GpgServerMessageType.class, GpgServerMessageTypeTypeAdapter.INSTANCE);
    gsonBuilder.registerTypeAdapter(MessageTarget.class, MessageTargetTypeAdapter.INSTANCE);
  }
}
