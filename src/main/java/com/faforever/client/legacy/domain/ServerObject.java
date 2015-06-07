package com.faforever.client.legacy.domain;

/**
 * Superclass for all server objects. Server objects are deserialized from a JSON-like string, therefore all field names
 * and types must exactly match to what the server sends.. A server object's concrete type is derived by its {@link
 * #command}.
 *
 * @see ServerObjectType
 */
public class ServerObject {

  /**
   * The server "command" actually isn't a command but identifies the object type.
   */
  public String command;
}
