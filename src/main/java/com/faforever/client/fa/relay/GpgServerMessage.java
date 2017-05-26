package com.faforever.client.fa.relay;

import com.faforever.client.net.SocketAddressUtil;
import com.faforever.client.remote.domain.MessageTarget;
import com.faforever.client.remote.domain.SerializableMessage;
import com.faforever.client.remote.domain.ServerMessage;

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

  protected GpgServerMessage(GpgServerMessageType command, int numberOfArgs) {
    this.command = command;
    this.args = new ArrayList<>(Collections.nCopies(numberOfArgs, null));
  }

  protected void setValue(int index, Object value) {
    args.set(index, value);
  }

  protected int getInt(int index) {
    return ((Number) args.get(index)).intValue();
  }

  protected boolean getBoolean(int index) {
    return (boolean) args.get(index);
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

  @SuppressWarnings("unchecked")
  protected <T> T getObject(int index) {
    return (T) args.get(index);
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

  public List<Object> getArgs() {
    return args;
  }
}
