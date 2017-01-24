package com.faforever.client.remote;

import com.faforever.client.remote.domain.ClientMessage;
import com.faforever.client.remote.domain.ClientMessageType;

public class PongMessage extends ClientMessage {

  protected PongMessage() {
    super(ClientMessageType.PONG);
  }
}
