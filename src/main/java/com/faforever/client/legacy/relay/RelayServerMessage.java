package com.faforever.client.legacy.relay;

import java.util.List;

/**
 * Represents a message received from the relay server (deserialized from JSON).
 */
public class RelayServerMessage {

  /**
   * Contains the command to execute, but the server sends it as "key".
   */
  public RelayServerCommand key;

  /**
   * Contains the arguments, but the server thinks it's cool to confuse us by calling it "commands".
   */
  public List<Object> commands;
}
