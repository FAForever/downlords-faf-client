package com.faforever.client.legacy.relay;


import com.faforever.client.legacy.domain.SerializableMessage;

import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class GpgClientMessage implements SerializableMessage {

  private String command;
  private List<Object> args;

  public GpgClientMessage(GpgClientCommand command, List<Object> args) {
    this(command.getString(), args);
  }

  public GpgClientMessage(String command, List<Object> args) {
    this.command = command;
    this.args = args;
  }

  public List<Object> getArgs() {
    return args;
  }

  public GpgClientCommand getCommand() {
    return GpgClientCommand.fromString(command);
  }

  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }
}
