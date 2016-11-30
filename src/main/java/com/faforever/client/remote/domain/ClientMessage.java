package com.faforever.client.remote.domain;

import java.util.Collection;
import java.util.Collections;

public class ClientMessage implements SerializableMessage {

  private ClientMessageType command;
  private MessageTarget target;

  protected ClientMessage(ClientMessageType command) {
    this.command = command;
  }

  @Override
  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }

  public ClientMessageType getCommand() {
    return command;
  }

  protected void setCommand(ClientMessageType command) {
    this.command = command;
  }

  public MessageTarget getTarget() {
    return target;
  }

  protected void setTarget(MessageTarget target) {
    this.target = target;
  }
}
