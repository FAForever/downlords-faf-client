package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.legacy.domain.SerializableMessage;
import com.faforever.client.legacy.domain.ServerMessage;
import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a message received from the relay server (deserialized from JSON).
 */
public class GpgServerMessage implements SerializableMessage, ServerMessage {

  private GpgServerMessageType command;
  private List<Object> args;
  private String jsonString;

  public GpgServerMessage() {
  }

  protected GpgServerMessage(GpgServerMessageType command, int numberOfArgs) {
    this.command = command;
    this.args = new ArrayList<>(Collections.nCopies(numberOfArgs, null));
  }

  public GpgServerMessage(GpgServerMessageType command, List<Object> args) {
    this.command = command;
    this.args = args;
  }

  /**
   * Returns what the server sends as "commands" but with a sane naming (args).
   */
  public List<Object> getArgs() {
    return Collections.unmodifiableList(args);
  }

  @VisibleForTesting
  void setArgs(List<Object> args) {
    this.args = args;
  }

  protected void setValue(int index, Object value) {
    args.set(index, value);
  }

  protected int getInt(int index) {
    return ((Number) args.get(index)).intValue();
  }

  protected String getString(int index) {
    return ((String) args.get(index));
  }

  @Override
  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }

  @Override
  public GpgServerMessageType getMessageType() {
    return command;
  }

  @Override
  public MessageTarget getTarget() {
    return null;
  }

  @Override
  public String getJsonString() {
    return jsonString;
  }

  @Override
  public void setJsonString(String jsonString) {
    this.jsonString = jsonString;
  }

  protected static int asInt(Object object) {
    return ((Double) object).intValue();
  }
}
