package com.faforever.client.remote.domain;

import com.faforever.client.relay.GpgClientCommand;
import com.faforever.client.relay.GpgGameMessage;

import java.util.Collections;

public class GameEndedMessage extends GpgGameMessage {

  public GameEndedMessage() {
    super(GpgClientCommand.GAME_STATE, Collections.singletonList("Ended"));
  }
}
