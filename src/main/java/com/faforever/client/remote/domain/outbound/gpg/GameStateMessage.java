package com.faforever.client.remote.domain.outbound.gpg;

import java.util.List;

public abstract class GameStateMessage extends GpgOutboundMessage {
  public static final String COMMAND = "GameState";

  public GameStateMessage(String state) {
    super(COMMAND, List.of(state));
  }
}
