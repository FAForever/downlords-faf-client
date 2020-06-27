package com.faforever.client.remote.domain;

import com.faforever.client.game.Faction;
import lombok.Getter;

@Getter
public class GameMatchmakingMessage extends ClientMessage {

  private final String queue_name;
  private final MatchmakingState state;
  private final Faction faction;

  public GameMatchmakingMessage(String queue_name, MatchmakingState state, Faction faction) {
    super(ClientMessageType.GAME_MATCHMAKING);
    this.queue_name = queue_name;
    this.state = state;
    this.faction = faction;
  }

}
