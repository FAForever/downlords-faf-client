package com.faforever.client.remote.domain;

import lombok.Getter;

@Getter
public class GameMatchmakingMessage extends ClientMessage {

  private final String queue_name;
  private final MatchmakingState state;

  public GameMatchmakingMessage(String queue_name, MatchmakingState state) {
    super(ClientMessageType.GAME_MATCHMAKING);
    this.queue_name = queue_name;
    this.state = state;
  }

}
