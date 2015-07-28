package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.domain.SerializableMessage;
import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a message received from the relay server (deserialized from JSON).
 */
class RelayServerMessage implements SerializableMessage {

  /**
   * Contains the command to execute, but the server sends it as "key".
   */
  private RelayServerCommand key;

  /**
   * Contains the arguments, but the server thinks it's cool to confuse us by calling it "commands".
   */
  private List<Object> commands;

  public RelayServerMessage() {
  }

  public RelayServerMessage(RelayServerCommand command) {
    this.key = command;
    this.commands = new ArrayList<>(Collections.nCopies(command.getNumberOfArgs(), null));
  }

  /**
   * Returns what the server sends as "key" but with a sane naming (command).
   */
  public RelayServerCommand getCommand() {
    return key;
  }

  @VisibleForTesting
  void setArgs(List<Object> args) {
    this.commands = args;
  }

  /**
   * Returns what the server sends as "commands" but with a sane naming (args).
   */
  public List<Object> getArgs() {
    return Collections.unmodifiableList(commands);
  }

  protected void setValue(int index, Object value) {
    commands.set(index, value);
  }

  protected int getInt(int index) {
    return ((Number) commands.get(index)).intValue();
  }

  protected String getString(int index) {
    return ((String) commands.get(index));
  }

  @Override
  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }

  protected static int asInt(Object object) {
    return ((Double) object).intValue();
  }
}
