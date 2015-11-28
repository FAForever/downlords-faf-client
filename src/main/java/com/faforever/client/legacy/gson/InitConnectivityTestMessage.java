package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ClientMessageType;
import com.faforever.client.legacy.domain.MessageTarget;

public class InitConnectivityTestMessage extends ClientMessage {

  private int port;

  public InitConnectivityTestMessage() {
    super(ClientMessageType.INIT_CONNECTIVITY_TEST);
    setTarget(MessageTarget.CONNECTIVITY);
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }
}
