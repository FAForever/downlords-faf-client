package com.faforever.client.legacy.domain;

import java.util.Collection;
import java.util.Collections;

/**
 * Superclass for all server objects. Server objects are deserialized from a JSON-like string, therefore all field names
 * and types must exactly match to what the server sends.. A server object's concrete type is derived by its {@link
 * #command}.
 *
 * @see ServerObjectType
 */
public class ServerObject implements SerializableMessage {

  /**
   * The server "command" actually isn't a command but identifies the object type.
   */
  public String command;

  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }
}
