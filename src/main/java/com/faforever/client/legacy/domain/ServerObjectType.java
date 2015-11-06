package com.faforever.client.legacy.domain;

import java.util.HashMap;
import java.util.Map;

public enum ServerObjectType {
  WELCOME("welcome"),
  GAME_INFO("game_info"),
  PLAYER_INFO("player_info"),
  GAME_LAUNCH("game_launch"),
  // TODO rename to GAME_TYPE_INFO?
  MOD_INFO("mod_info"),
  TUTORIALS_INFO("tutorials_info"),
  MATCHMAKER_INFO("matchmaker_info"),
  SOCIAL("social"),
  STATS("stats");

  private static final Map<String, ServerObjectType> fromString;

  static {
    fromString = new HashMap<>(values().length, 1);
    for (ServerObjectType serverObjectType : values()) {
      fromString.put(serverObjectType.string, serverObjectType);
    }
  }

  private String string;

  ServerObjectType(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }

  public static ServerObjectType fromString(String string) {
    return fromString.get(string);
  }
}
