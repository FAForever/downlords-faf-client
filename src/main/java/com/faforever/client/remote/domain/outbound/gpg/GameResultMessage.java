package com.faforever.client.remote.domain.outbound.gpg;




public class GameResultMessage extends GpgOutboundMessage {
  public static final String COMMAND = "GameResult";

  public GameResultMessage() {
    super(COMMAND);
  }
}
