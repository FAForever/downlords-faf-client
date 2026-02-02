package com.faforever.client.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LogMaskingRegistry {
  private static final Map<SensitiveValueType, AtomicReference<List<String>>> STORE = new ConcurrentHashMap<>();

  static {
    for (SensitiveValueType type : SensitiveValueType.values()) {
      STORE.put(type, new AtomicReference<>(List.of()));
    }
  }

  /**
   * Replace the masked value for a given secret type. Keeps at most 2 values (current + previous).
   */
  public static void update(SensitiveValueType type, String newValue) {
    if (newValue == null || newValue.isBlank()) {
      return;
    }

    STORE.get(type).getAndUpdate(existing -> {
      if (existing.isEmpty()) {
        return List.of(newValue);
      }
      if (existing.getFirst().equals(newValue)) {
        return existing;
      }
      return List.of(newValue, existing.getFirst());
    });
  }

  /**
   * Masks log message
   */
  static String maskMessage(String message) {
    if (message == null || message.isEmpty()) {return message;}

    String maskedMessage = message;
    for (LogMaskingRegistry.SensitiveValueType type : LogMaskingRegistry.SensitiveValueType.values()) {
      List<String> secrets = LogMaskingRegistry.STORE.get(type).get();
      if (secrets.isEmpty()) {continue;}
      for (String secret : secrets) {
        maskedMessage = maskedMessage.replace(secret, "%" + type.name() + "%");
      }
    }
    return maskedMessage;
  }

  public enum SensitiveValueType {
    ACCESS_TOKEN, REFRESH_TOKEN, HMAC
  }

}

