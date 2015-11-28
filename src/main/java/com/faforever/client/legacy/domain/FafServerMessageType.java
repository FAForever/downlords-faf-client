package com.faforever.client.legacy.domain;

import com.faforever.client.legacy.UpdatedAchievementsMessageLobby;
import com.faforever.client.legacy.relay.ConnectivityStateMessage;
import com.faforever.client.rankedmatch.MatchmakerLobbyServerMessage;
import com.faforever.client.stats.StatisticsMessageLobby;

import java.util.HashMap;
import java.util.Map;

public enum FafServerMessageType implements ServerMessageType {
  WELCOME("welcome", LoginLobbyServerMessage.class),
  SESSION("session", SessionMessageLobby.class),
  GAME_INFO("game_info", GameInfoMessage.class),
  PLAYER_INFO("player_info", PlayersMessageLobby.class),
  GAME_LAUNCH("game_launch", GameLaunchMessageLobby.class),
  GAME_TYPE_INFO("mod_info", GameTypeMessage.class),
  MATCHMAKER_INFO("matchmaker_info", MatchmakerLobbyServerMessage.class),
  SOCIAL("social", SocialMessageLobby.class),
  AUTHENTICATION_FAILED("authentication_failed", AuthenticationFailedMessageLobby.class),
  STATS("stats", StatisticsMessageLobby.class),
  UPDATED_ACHIEVEMENTS("updated_achievements", UpdatedAchievementsMessageLobby.class),
  CONNECTIVITY_STATE("ConnectivityState", ConnectivityStateMessage.class);

  private static final Map<String, FafServerMessageType> fromString;

  static {
    fromString = new HashMap<>(values().length, 1);
    for (FafServerMessageType fafServerMessageType : values()) {
      fromString.put(fafServerMessageType.string, fafServerMessageType);
    }
  }

  private final String string;
  private final Class<? extends FafServerMessage> type;

  FafServerMessageType(String string, Class<? extends FafServerMessage> type) {
    this.string = string;
    this.type = type;
  }

  @Override
  public String getString() {
    return string;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ServerMessage> Class<T> getType() {
    return (Class<T>) type;
  }

  public static FafServerMessageType fromString(String string) {
    return fromString.get(string);
  }

}
