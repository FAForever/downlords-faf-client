package com.faforever.client.remote.domain.outbound.faf;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;


@EqualsAndHashCode(callSuper = true)
@Value
public class RestoreGameSessionMessage extends FafOutboundMessage {
  public static final String COMMAND = "restore_game_session";

  @JsonProperty("game_id")
  int gameId;
}
