package com.faforever.client.legacy.domain;

import java.util.HashMap;
import java.util.Map;

public enum ServerMessageType {
  WELCOME("welcome"),
  GAME_INFO("game_info"),
  PLAYER_INFO("player_info"),
  GAME_LAUNCH("game_launch"),
  GAME_TYPE_INFO("mod_info"),
  TUTORIALS_INFO("tutorials_info"),
  MATCHMAKER_INFO("matchmaker_info"),
  MOD_VAULT_INFO("modvault_info"),
  SOCIAL("social"),
  STATS("stats"),
  UPDATED_ACHIEVEMENTS("updated_achievements"),
  MOD_RESULT_LIST("modvault_list_info");

  private static final Map<String, ServerMessageType> fromString;

  static {
    fromString = new HashMap<>(values().length, 1);
    for (ServerMessageType serverMessageType : values()) {
      fromString.put(serverMessageType.string, serverMessageType);
    }
  }

  private final String string;

  ServerMessageType(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }

  public static ServerMessageType fromString(String string) {
    return fromString.get(string);
  }

}
