package com.faforever.client.remote.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

import java.lang.reflect.Type;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LocalTimeDeserializer implements JsonDeserializer<LocalTime> {

  public static final LocalTimeDeserializer INSTANCE = new LocalTimeDeserializer();
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private LocalTimeDeserializer() {

  }

  @Override
  public LocalTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
    return LocalTime.parse(json.getAsString(), FORMATTER);
  }
}
