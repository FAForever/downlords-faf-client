package com.faforever.client.legacy.domain;

import java.util.HashMap;
import java.util.Map;

public enum ClientMessageType {
  ASK_SESSION("ask_session"),
  // TODO fix the naming
  GAME_MATCH_MAKING("game_matchmaking");

  private static final Map<String, ClientMessageType> fromString;
  private String string;

  ClientMessageType(String string) {
    this.string = string;
  }

  static {
    fromString = new HashMap<>(values().length, 1);
    for (ClientMessageType clientMessageType : values()) {
      fromString.put(clientMessageType.string, clientMessageType);
    }
  }

  public String getString() {
    return string;
  }

  public static ClientMessageType fromString(String string) {
    return fromString.get(string);
  }

}
