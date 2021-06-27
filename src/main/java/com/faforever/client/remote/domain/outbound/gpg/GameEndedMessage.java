package com.faforever.client.remote.domain.outbound.gpg;

import lombok.Builder;

public class GameEndedMessage extends GameStateMessage {


  public GameEndedMessage() {
    super("Ended");
  }
}
