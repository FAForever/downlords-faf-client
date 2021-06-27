package com.faforever.client.remote.domain.outbound.gpg;

import lombok.Builder;


public class GameModsMessage extends GpgOutboundMessage {
  public static final String COMMAND = "GameMods";

  public GameModsMessage() {
    super(COMMAND);
  }
}
