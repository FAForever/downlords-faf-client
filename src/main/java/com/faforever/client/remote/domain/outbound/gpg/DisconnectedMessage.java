package com.faforever.client.remote.domain.outbound.gpg;




public class DisconnectedMessage extends GpgOutboundMessage {
  public static final String COMMAND = "Disconnected";

  public DisconnectedMessage() {
    super(COMMAND);
  }
}
