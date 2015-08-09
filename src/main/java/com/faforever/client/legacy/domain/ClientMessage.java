package com.faforever.client.legacy.domain;

import java.util.Collection;
import java.util.Collections;

public abstract class ClientMessage implements SerializableMessage {

  private String action;
  private String command;

  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }
}
