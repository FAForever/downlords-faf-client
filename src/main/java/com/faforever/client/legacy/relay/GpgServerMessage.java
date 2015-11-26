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
class GpgServerMessage implements SerializableMessage {

  /**
   * Contains the command to execute, but the server sends it as "key".
   */
  private GpgServerCommandServerCommand key;

  /**
   * Contains the arguments, but the server thinks it's cool to confuse us by calling it "commands".
   */
  private List<Object> commands;

  public GpgServerMessage() {
  }

  public GpgServerMessage(GpgServerCommandServerCommand command) {
    this.key = command;
    this.commands = new ArrayList<>(Collections.nCopies(command.getNumberOfArgs(), null));
  }

  /**
   * Returns what the server sends as "key" but with a sane naming (command).
   */
  public GpgServerCommandServerCommand getCommand() {
    return key;
  }

  /**
   * Returns what the server sends as "commands" but with a sane naming (args).
   */
  public List<Object> getArgs() {
    return Collections.unmodifiableList(commands);
  }

  @VisibleForTesting
  void setArgs(List<Object> args) {
    this.commands = args;
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
