package com.faforever.client.remote.domain.outbound.gpg;




public class PlayerOptionMessage extends GpgOutboundMessage {
  public static final String COMMAND = "PlayerOption";

  public PlayerOptionMessage() {
    super(COMMAND);
  }
}
