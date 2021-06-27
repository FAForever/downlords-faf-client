package com.faforever.client.remote.domain.outbound.gpg;

import lombok.Builder;


public class RehostMessage extends GpgOutboundMessage {
  public static final String COMMAND = "Rehost";

  public RehostMessage() {
    super(COMMAND);
  }
}
