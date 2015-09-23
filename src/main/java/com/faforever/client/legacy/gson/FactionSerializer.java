package com.faforever.client.legacy.gson;

import com.faforever.client.game.Faction;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class FactionSerializer implements JsonSerializer<Faction> {

  @Override
  public JsonElement serialize(Faction src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.getId());
  }
}
