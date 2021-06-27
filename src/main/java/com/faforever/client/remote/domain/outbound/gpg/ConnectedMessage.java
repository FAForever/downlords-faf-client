package com.faforever.client.remote.domain.outbound.gpg;

import lombok.Builder;


public class ConnectedMessage extends GpgOutboundMessage {
  public static final String COMMAND = "Connected";

  public ConnectedMessage() {
    super(COMMAND);
  }
}
