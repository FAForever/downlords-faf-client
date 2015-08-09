package com.faforever.client.legacy.relay;

import java.util.HashMap;
import java.util.Map;

public enum LobbyAction {
  PROCESS_NAT_PACKET("ProcessNatPacket"),
  DISCONNECTED("Disconnected"),
  CONNECTED("Connected"),
  GAME_STATE("GameState"),
  BOTTLENECK("Bottleneck"),
  BOTTLENECK_CLEARED("BottleneckCleared"),
  GAME_OPTION("GameOption"),
  GAME_MODS("GameMods"),
  PLAYER_OPTION("PlayerOption"),
  DISCONNECT_FROM_PEER("DisconnectFromPeer"),
  CHAT("Chat"),
  GAME_RESULT("GameResult"),
  STATS("Stats"),
  AUTHENTICATE("Authenticate"),
  // Yes, these are the only lower-cased in the protocol. Because fuck you.
  CONNECTED_TO_HOST("connectedToHost"),
  PONG("pong");

  private static final Map<String, LobbyAction> fromString;
  static {
    fromString = new HashMap<>();
    for (LobbyAction action : values()) {
      fromString.put(action.string, action);
    }
  }

  private final String string;

  LobbyAction(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }

  public static LobbyAction fromString(String string) {
    LobbyAction action = fromString.get(string);
    if (action == null) {
      throw new IllegalArgumentException("Unknown relay server action: " + string);
    }
    return action;
  }
}
