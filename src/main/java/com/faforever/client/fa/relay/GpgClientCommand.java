package com.faforever.client.fa.relay;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public enum GpgClientCommand {
  DISCONNECTED("Disconnected"),
  CONNECTED("Connected"),
  GAME_STATE("GameState"),
  GAME_OPTION("GameOption"),
  GAME_MODS("GameMods"),
  PLAYER_OPTION("PlayerOption"),
  DISCONNECT_FROM_PEER("DisconnectFromPeer"),
  CHAT("Chat"),
  GAME_RESULT("GameResult"),
  STATS("Stats"),
  CLEAR_SLOT("ClearSlot"),
  AI_OPTION("AIOption"),
  JSON_STATS("JsonStats"),
  REHOST("Rehost"),
  DESYNC("Desync"),
  GAME_FULL("GameFull"),
  GAME_ENDED("GameEnded"),
  ICE_MESSAGE("IceMsg"),
  // Yes, this is the only lower-cased command in the protocol. Because reasons.
  CONNECTED_TO_HOST("connectedToHost");

  private static final Map<String, GpgClientCommand> fromString;

  static {
    fromString = new HashMap<>();
    for (GpgClientCommand action : values()) {
      fromString.put(action.string, action);
    }
  }

  private final String string;

  GpgClientCommand(String string) {
    this.string = string;
  }

  public static GpgClientCommand fromString(String string) {
    GpgClientCommand action = fromString.get(string);
    if (action == null) {
      log.warn("Unknown lobby action: {}", string);
    }
    return action;
  }

  public String getString() {
    return string;
  }
}
