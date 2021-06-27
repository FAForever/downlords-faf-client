package com.faforever.client.remote.domain.outbound.gpg;

import lombok.Builder;


public class DesyncMessage extends GpgOutboundMessage {
  public static final String COMMAND = "Desync";

  public DesyncMessage() {
    super(COMMAND);
  }
}
