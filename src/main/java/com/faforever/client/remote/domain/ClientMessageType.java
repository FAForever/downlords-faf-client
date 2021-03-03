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
  OAUTH_LOGIN("auth"),
  GAME_MATCH_MAKING("game_matchmaking"),
  AVATAR("avatar"),
  ICE_SERVERS("ice_servers"),
  RESTORE_GAME_SESSION("restore_game_session"),
  PING("ping"),
  PONG("pong"),
  ADMIN("admin"),
  INVITE_TO_PARTY("invite_to_party"),
  ACCEPT_PARTY_INVITE("accept_party_invite"),
  KICK_PLAYER_FROM_PARTY("kick_player_from_party"),
  READY_PARTY("ready_party"),
  UNREADY_PARTY("unready_party"),
  LEAVE_PARTY("leave_party"),
  SET_PARTY_FACTIONS("set_party_factions"),
  MATCHMAKER_INFO("matchmaker_info"),
  GAME_MATCHMAKING("game_matchmaking");

  private static final Map<String, ClientMessageType> fromString;

  static {
    fromString = new HashMap<>();
    for (ClientMessageType clientMessageType : values()) {
      fromString.put(clientMessageType.string, clientMessageType);
    }
  }

  private final String string;

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
