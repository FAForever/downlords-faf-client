package com.faforever.client.remote.domain.outbound.gpg;

import lombok.Builder;


public class GameFullMessage extends GpgOutboundMessage {
  public static final String COMMAND = "GameFull";

  public GameFullMessage() {
    super(COMMAND);
  }
}
