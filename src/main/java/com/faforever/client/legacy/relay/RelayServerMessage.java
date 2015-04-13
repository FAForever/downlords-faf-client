package com.faforever.client.legacy.relay;

import java.util.List;

/**
 * Represents a message received from the relay server.
 */
public class RelayServerMessage {

  // This is the actual "command"
  public String key;

  // Actually, this is a list of arguments, but having non-sense names makes everything easier
  public List<Object> commands;
}
