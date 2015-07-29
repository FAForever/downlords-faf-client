package com.faforever.client.legacy.relay;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class RelayServerActionDeserializer implements JsonDeserializer<LobbyAction> {

  @Override
  public LobbyAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    return LobbyAction.fromString(json.getAsString());
  }
}
