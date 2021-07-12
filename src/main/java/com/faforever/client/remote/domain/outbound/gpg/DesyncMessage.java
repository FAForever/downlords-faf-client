package com.faforever.client.remote.domain.outbound.gpg;




public class DesyncMessage extends GpgOutboundMessage {
  public static final String COMMAND = "Desync";

  public DesyncMessage() {
    super(COMMAND);
  }
}
