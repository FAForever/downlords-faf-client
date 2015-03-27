package com.faforever.client.legacy;

import java.util.HashMap;
import java.util.Map;

public enum MessageType {
  PING,
  LOGIN_AVAILABLE,
  ACK,
  ERROR,
  MESSAGE;

  private static final Map<String, MessageType> fromString;

  static {
    fromString = new HashMap<>(values().length, 1);
    for (MessageType messageType : values()) {
      fromString.put(messageType.name(), messageType);
    }
  }

  public static MessageType fromString(String string) {
    return fromString.get(string);
  }
}
