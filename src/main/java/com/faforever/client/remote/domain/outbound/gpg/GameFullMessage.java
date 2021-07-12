package com.faforever.client.remote.domain.outbound.gpg;




public class GameFullMessage extends GpgOutboundMessage {
  public static final String COMMAND = "GameFull";

  public GameFullMessage() {
    super(COMMAND);
  }
}
