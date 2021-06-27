package com.faforever.client.remote.domain.outbound.gpg;

import lombok.Builder;


public class GameOptionMessage extends GpgOutboundMessage {
  public static final String COMMAND = "GameOption";

  public GameOptionMessage() {
    super(COMMAND);
  }
}
