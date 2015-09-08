package com.faforever.client.legacy.domain;

import java.util.Collection;
import java.util.Collections;

public class ClientMessage implements SerializableMessage {

  private ClientMessageType command;

  protected ClientMessage(ClientMessageType command) {
    this.command = command;
  }

  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }

  public ClientMessageType getCommand() {
    return command;
  }

  public void setCommand(ClientMessageType command) {
    this.command = command;
  }
}
