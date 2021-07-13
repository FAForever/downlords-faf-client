package com.faforever.client.remote.domain.outbound.gpg;




public class GameModsMessage extends GpgOutboundMessage {
  public static final String COMMAND = "GameMods";

  public GameModsMessage() {
    super(COMMAND);
  }
}
