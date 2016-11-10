package com.faforever.client.relay;

import com.faforever.client.remote.domain.MessageTarget;

public class HostGameMessage extends GpgServerMessage {

  private static final int MAP_INDEX = 0;

  public HostGameMessage() {
    super(GpgServerMessageType.HOST_GAME, 1);
    setTarget(MessageTarget.GAME);
  }

  public String getMap() {
    return getString(MAP_INDEX);
  }
}
