package com.faforever.client.remote.domain;

import com.faforever.client.rankedmatch.MatchmakerInfoMessage;
import com.faforever.client.remote.UpdatedAchievementsMessage;

import java.util.HashMap;
import java.util.Map;

public enum FafServerMessageType implements ServerMessageType {
  WELCOME("welcome", LoginMessage.class),
  SESSION("session", SessionMessage.class),
  GAME_INFO("game_info", GameInfoMessage.class),
  PLAYER_INFO("player_info", PlayersMessage.class),
  GAME_LAUNCH("game_launch", GameLaunchMessage.class),
  MATCHMAKER_INFO("matchmaker_info", MatchmakerInfoMessage.class),
  SOCIAL("social", SocialMessage.class),
  AUTHENTICATION_FAILED("authentication_failed", AuthenticationFailedMessage.class),
  UPDATED_ACHIEVEMENTS("updated_achievements", UpdatedAchievementsMessage.class),
  NOTICE("notice", NoticeMessage.class),
  ICE_SERVERS("ice_servers", IceServersServerMessage.class),
  AVATAR("avatar", AvatarMessage.class);

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

  public static FafServerMessageType fromString(String string) {
    return fromString.get(string);
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

}
