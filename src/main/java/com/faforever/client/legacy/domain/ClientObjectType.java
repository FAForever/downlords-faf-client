package com.faforever.client.legacy.domain;

import java.util.HashMap;
import java.util.Map;

public enum ClientObjectType {
  ASK_SESSION("ask_session"),
  // TODO fix the naming
  ACCEPT_1V1_MATCH("game_matchmaking");

  private static final Map<String, ClientObjectType> fromString;
  private String string;

  ClientObjectType(String string) {
    this.string = string;
  }

  static {
    fromString = new HashMap<>(values().length, 1);
    for (ClientObjectType clientObjectType : values()) {
      fromString.put(clientObjectType.string, clientObjectType);
    }
  }

  public String getString() {
    return string;
  }

  public static ClientObjectType fromString(String string) {
    return fromString.get(string);
  }

}
