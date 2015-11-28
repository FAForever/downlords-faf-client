package com.faforever.client.legacy.domain;

import java.util.HashMap;
import java.util.Map;

public enum ClientMessageType {
  HOST_GAME("game_host"),
  LIST_REPLAYS("list"),
  JOIN_GAME("game_join"),
  ASK_SESSION("ask_session"),
  SOCIAL("social"),
  STATISTICS("stats"),
  LOGIN("hello"),
  GAME_MATCH_MAKING("game_matchmaking"),
  MOD_VAULT("modvault"),
  MOD_VAULT_SEARCH("modvault_search"),
  INIT_CONNECTIVITY_TEST("InitiateTest");

  private static Map<String, ClientMessageType> fromString;

  static {
    fromString = new HashMap<>();
    for (ClientMessageType clientMessageType : values()) {
      fromString.put(clientMessageType.string, clientMessageType);
    }
  }

  private String string;

  ClientMessageType(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }

  public static ClientMessageType fromString(String string) {
    return fromString.get(string);
  }
}
