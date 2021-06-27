package com.faforever.client.remote.domain.outbound.faf;

import com.faforever.client.remote.domain.MatchmakingState;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;


@EqualsAndHashCode(callSuper = true)
@Value
public class GameMatchmakingMessage extends FafOutboundMessage {
  public static final String COMMAND = "game_matchmaking";

  String queueName;
  MatchmakingState state;
}
