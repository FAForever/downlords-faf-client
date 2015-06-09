package com.faforever.client.legacy.relay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a message received from the relay server (deserialized from JSON).
 */
class RelayServerMessage {

  /**
   * Contains the command to execute, but the server sends it as "key".
   */
  protected RelayServerCommand key;

  /**
   * Contains the arguments, but the server thinks it's cool to confuse us by calling it "commands".
   */
  protected List<Object> commands;

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

  /**
   * Returns what the server sends as "commands" but with a sane naming (args).
   */
  public List<Object> getArgs() {
    return commands;
  }

  protected static int asInt(Object object) {
    return ((Double) object).intValue();
  }
}
