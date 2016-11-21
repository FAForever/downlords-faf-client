package com.faforever.client.remote.domain;

import com.faforever.client.update.Version;

import java.util.Collection;
import java.util.Collections;

public class ClientMessage implements SerializableMessage {

  private static final String USER_AGENT = "downlords-faf-client-" + Version.VERSION;

  private ClientMessageType command;
  private MessageTarget target;
  private String userAgent = USER_AGENT;

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
