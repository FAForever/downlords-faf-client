package com.faforever.client.remote.domain.outbound.gpg;

import lombok.Builder;


public class ConnectedToHostMessage extends GpgOutboundMessage {
  public static final String COMMAND = "connectedToHost";

  public ConnectedToHostMessage() {
    super(COMMAND);
  }
}
