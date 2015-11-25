package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.writer.JsonMessageSerializer;
import com.google.gson.GsonBuilder;

public class GpgServerCommandMessageSerializer extends JsonMessageSerializer<GpgpServerMessage> {

  @Override
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    super.addTypeAdapters(gsonBuilder);

    gsonBuilder.registerTypeAdapter(GpgServerCommandServerCommand.class, GpgServerCommandTypeAdapter.INSTANCE);
  }
}
