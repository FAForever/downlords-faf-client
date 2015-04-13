package com.faforever.client.legacy;

import java.util.HashMap;
import java.util.Map;

public enum ServerCommand {
  WELCOME("welcome"),
  GAME_INFO("game_info"),
  PLAYER_INFO("player_info"),
  GAME_LAUNCH("game_launch"),
  MOD_INFO("mod_info"),
  TUTORIALS_INFO("tutorials_info"),
  MATCHMAKER_INFO("matchmaker_info");

  private static final Map<String, ServerCommand> fromString;
  private String string;

  ServerCommand(String string) {
    this.string = string;
  }

  static {
    fromString = new HashMap<>(values().length, 1);
    for (ServerCommand serverCommand : values()) {
      fromString.put(serverCommand.string, serverCommand);
    }
  }

  public static ServerCommand fromString(String string) {
    return fromString.get(string);
  }

}
