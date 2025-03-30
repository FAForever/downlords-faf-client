package com.faforever.client.util;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DeepCopyUtil {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static <T> T deepCopy(T object) {
    try {
      return objectMapper.readValue(objectMapper.writeValueAsString(object), (Class<T>) object.getClass());
    } catch (Exception e) {
      throw new RuntimeException("Deep copy failed", e);
    }
  }
}