package com.faforever.client.remote.domain.outbound.gpg;




public class ConnectedMessage extends GpgOutboundMessage {
  public static final String COMMAND = "Connected";

  public ConnectedMessage() {
    super(COMMAND);
  }
}
