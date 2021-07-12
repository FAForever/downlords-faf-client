package com.faforever.client.remote.domain.outbound.gpg;




public class GameOptionMessage extends GpgOutboundMessage {
  public static final String COMMAND = "GameOption";

  public GameOptionMessage() {
    super(COMMAND);
  }
}
