package com.faforever.client.remote.domain.outbound.gpg;




public class ConnectedToHostMessage extends GpgOutboundMessage {
  public static final String COMMAND = "connectedToHost";

  public ConnectedToHostMessage() {
    super(COMMAND);
  }
}
