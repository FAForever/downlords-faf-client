package com.faforever.client.legacy.domain;

import java.util.Collection;
import java.util.Collections;

public class ClientMessage implements SerializableMessage {

  private String action;
  private ClientMessageType command;

  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public ClientMessageType getCommand() {
    return command;
  }

  public void setCommand(ClientMessageType command) {
    this.command = command;
  }
}
