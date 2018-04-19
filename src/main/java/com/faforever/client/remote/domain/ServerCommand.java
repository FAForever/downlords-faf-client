package com.faforever.client.remote.domain;

import java.util.HashMap;
import java.util.Map;

public enum ServerCommand {
  PING,
  PONG,
  LOGIN_AVAILABLE,
  ACK,
  ERROR,
  MESSAGE;

  private static final Map<String, ServerCommand> fromString;

  static {
    fromString = new HashMap<>(values().length, 1);
    for (ServerCommand serverCommand : values()) {
      fromString.put(serverCommand.name(), serverCommand);
    }
  }

  public static ServerCommand fromString(String string) {
    return fromString.get(string);
  }
}
