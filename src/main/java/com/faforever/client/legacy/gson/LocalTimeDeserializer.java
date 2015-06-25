package com.faforever.client.legacy.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LocalTimeDeserializer implements JsonDeserializer<LocalTime> {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  @Override
  public LocalTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    return LocalTime.parse(json.getAsString(), FORMATTER);
  }
}
