package com.faforever.client.remote.domain;

import java.util.HashMap;
import java.util.Map;

public enum ClientMessageType {
  HOST_GAME("game_host"),
  LIST_REPLAYS("list"),
  JOIN_GAME("game_join"),
  ASK_SESSION("ask_session"),
  SOCIAL_ADD("social_add"),
  SOCIAL_REMOVE("social_remove"),
  STATISTICS("stats"),
  LOGIN("hello"),
  GAME_MATCH_MAKING("game_matchmaking"),
  AVATAR("avatar"),
  ICE_SERVERS("ice_servers"),
  RESTORE_GAME_SESSION("restore_game_session"),
  PING("ping"),
  PONG("pong"),
  ADMIN("admin"),
  MATCHMAKER_INFO("matchmaker_info");

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

  public static ClientMessageType fromString(String string) {
    return fromString.get(string);
  }

  public String getString() {
    return string;
  }
}
