package com.faforever.client.remote.domain;

import lombok.Getter;

@Getter
public class GameMatchmakingMessage extends ClientMessage {

  private final String queueName;
  private final MatchmakingState state;

  public GameMatchmakingMessage(String queueName, MatchmakingState state) {
    super(ClientMessageType.GAME_MATCHMAKING);
    this.queueName = queueName;
    this.state = state;
  }

}
