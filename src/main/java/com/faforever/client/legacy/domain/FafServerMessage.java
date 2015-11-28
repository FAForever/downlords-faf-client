package com.faforever.client.legacy.domain;

import java.util.Collection;
import java.util.Collections;

/**
 * Superclass for all server objects. Server objects are deserialized from a JSON-like string, therefore all field names
 * and types must exactly match to what the server sends.. A server object's concrete type is derived by its {@link
 * #command}.
 *
 * @see FafServerMessageType
 */
public class FafServerMessage implements SerializableMessage, ServerMessage {

  /**
   * The server "command" actually isn't a command but identifies the object type.
   */
  private FafServerMessageType command;
  private MessageTarget target;
  private String jsonString;

  protected FafServerMessage(FafServerMessageType command) {
    this();
    this.command = command;
  }

  public FafServerMessage() {
    target = MessageTarget.CLIENT;
  }

  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }

  @Override
  public FafServerMessageType getMessageType() {
    return command;
  }

  @Override
  public MessageTarget getTarget() {
    return target;
  }

  public void setTarget(MessageTarget target) {
    this.target = target;
  }

  @Override
  public String getJsonString() {
    return jsonString;
  }

  public void setJsonString(String jsonString) {
    this.jsonString = jsonString;
  }

  @Override
  public String toString() {
    return jsonString;
  }
}
