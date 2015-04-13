package com.faforever.client.legacy.message;

import java.util.HashMap;
import java.util.Map;

public enum ServerMessageType {
  PING,
  LOGIN_AVAILABLE,
  ACK,
  ERROR,
  MESSAGE;

  private static final Map<String, ServerMessageType> fromString;

  static {
    fromString = new HashMap<>(values().length, 1);
    for (ServerMessageType serverMessageType : values()) {
      fromString.put(serverMessageType.name(), serverMessageType);
    }
  }

  public static ServerMessageType fromString(String string) {
    return fromString.get(string);
  }
}
