package com.faforever.client.remote.domain;

import com.faforever.client.fa.relay.GpgClientCommand;
import com.faforever.client.fa.relay.GpgGameMessage;

import java.util.Arrays;

public class IceMessage extends GpgGameMessage {
  public IceMessage(int remotePlayerId, Object message) {
    super(GpgClientCommand.ICE_MESSAGE, Arrays.asList(remotePlayerId, message));
  }
}
