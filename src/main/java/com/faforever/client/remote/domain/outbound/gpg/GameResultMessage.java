package com.faforever.client.remote.domain.outbound.gpg;

import lombok.Builder;


public class GameResultMessage extends GpgOutboundMessage {
  public static final String COMMAND = "GameResult";

  public GameResultMessage() {
    super(COMMAND);
  }
}
