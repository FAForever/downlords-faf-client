package com.faforever.client.replay;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class ReplayActionJsonDeserializer implements JsonDeserializer<ReplayAction> {

  @Override
  public ReplayAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    return ReplayAction.fromString(json.getAsString());
  }
}
