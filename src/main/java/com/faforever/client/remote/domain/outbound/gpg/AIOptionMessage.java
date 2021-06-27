package com.faforever.client.remote.domain.outbound.gpg;

import lombok.Builder;


public class AIOptionMessage extends GpgOutboundMessage {
  public static final String COMMAND = "AIOption";

  public AIOptionMessage() {
    super(COMMAND);
  }
}
