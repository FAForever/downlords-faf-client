package com.faforever.client;

import com.faforever.client.game.GameInfoMessageTestBuilder;
import com.faforever.client.serialization.FactionMixin;
import com.faforever.commons.api.dto.Faction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.CaseFormat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class SnakeCaseFieldBuilder {

  public static void main(String[] args) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, JsonProcessingException {
//    Class<?> messageClass = ClassLoader.getSystemClassLoader().loadClass("com.faforever.client.remote.domain.outbound.faf.SelectAvatarMessage");

    ObjectMapper objectMapper = new ObjectMapper()
        .addMixIn(Faction.class, FactionMixin.class)
        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
        .enable(SerializationFeature.INDENT_OUTPUT);


    System.out.println(objectMapper.writeValueAsString(GameInfoMessageTestBuilder.create(1)
        .defaultValues()
        .get()));
//    printSnakeFieldsForClass(messageClass);
  }

  private static void printSnakeFieldsForClass(Class<?> clazz) throws IllegalAccessException {
    if (clazz == null) {
      return;
    }
    for (Field field : clazz.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers())) {
        System.out.println("\"" + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName()) + "\",");
      }
    }
    try {
      System.out.println("\"" + clazz.getField("COMMAND").get(null) + "\",");
    } catch (NoSuchFieldException ignored) {

    }
    printSnakeFieldsForClass(clazz.getSuperclass());
  }
}
