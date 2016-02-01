package com.faforever.client.relay;

import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.legacy.domain.SerializableMessage;
import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.net.SocketAddressUtil;
import com.google.common.annotations.VisibleForTesting;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a message received from the relay server (deserialized from JSON).
 */
public class GpgServerMessage implements SerializableMessage, ServerMessage {

  private GpgServerMessageType command;
  private MessageTarget target;
  private List<Object> args;

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

  protected InetSocketAddress getSocketAddress(int index) {
    // TODO remove this when the representation of addresses is finally unified on server side
    Object arg = args.get(index);
    if (arg instanceof String) {
      return SocketAddressUtil.fromString((String) arg);
    }

    @SuppressWarnings("unchecked")
    List<Object> addressArray = (List<Object>) arg;
    // TODO remove this when fixed on server side
    int port;
    if (addressArray.get(1) instanceof String) {
      port = Integer.parseInt((String) addressArray.get(1));
    } else {
      port = ((Number) addressArray.get(1)).intValue();
    }
    return new InetSocketAddress((String) addressArray.get(0), port);
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
    return target;
  }

  public void setTarget(MessageTarget target) {
    this.target = target;
  }
}
