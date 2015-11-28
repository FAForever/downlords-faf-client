package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.FafServerMessage;
import com.faforever.client.legacy.domain.FafServerMessageType;
import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.legacy.gson.MessageTargetTypeAdapter;
import com.faforever.client.legacy.gson.ServerMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.StatisticsTypeTypeAdapter;
import com.faforever.client.legacy.writer.JsonMessageSerializer;
import com.google.gson.GsonBuilder;

public class ServerMessageSerializer extends JsonMessageSerializer<FafServerMessage> {

  @Override
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    super.addTypeAdapters(gsonBuilder);

    gsonBuilder.registerTypeAdapter(MessageTarget.class, MessageTargetTypeAdapter.INSTANCE);
    gsonBuilder.registerTypeAdapter(FafServerMessageType.class, ServerMessageTypeTypeAdapter.INSTANCE);
    gsonBuilder.registerTypeAdapter(StatisticsType.class, StatisticsTypeTypeAdapter.INSTANCE);
  }
}
