package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.legacy.domain.ServerMessageType;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.legacy.gson.ServerMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.StatisticsTypeTypeAdapter;
import com.faforever.client.legacy.writer.JsonMessageSerializer;
import com.google.gson.GsonBuilder;

public class ServerMessageSerializer extends JsonMessageSerializer<ServerMessage> {

  @Override
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    super.addTypeAdapters(gsonBuilder);

    gsonBuilder.registerTypeAdapter(ServerMessageType.class, new ServerMessageTypeTypeAdapter());
    gsonBuilder.registerTypeAdapter(StatisticsType.class, new StatisticsTypeTypeAdapter());
  }
}
