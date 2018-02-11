package com.faforever.client.legacy;

import com.faforever.client.game.Faction;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class FactionDeserializer implements JsonDeserializer<Faction> {

  @Override
  public Faction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    return Faction.fromString(json.getAsString());
  }
}
