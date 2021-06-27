package com.faforever.client.remote.domain.outbound.gpg;

import lombok.Builder;


public class PlayerOptionMessage extends GpgOutboundMessage {
  public static final String COMMAND = "PlayerOption";

  public PlayerOptionMessage() {
    super(COMMAND);
  }
}
