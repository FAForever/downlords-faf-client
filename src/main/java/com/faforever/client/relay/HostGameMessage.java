package com.faforever.client.relay;

import com.faforever.client.legacy.domain.MessageTarget;

public class HostGameMessage extends GpgServerMessage {

  public HostGameMessage() {
    super(GpgServerMessageType.HOST_GAME, 0);
    setTarget(MessageTarget.GAME);
  }
}
