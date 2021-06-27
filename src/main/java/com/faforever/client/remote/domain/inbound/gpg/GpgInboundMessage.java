package com.faforever.client.remote.domain.inbound.gpg;

import com.faforever.client.remote.domain.MessageTarget;
import com.faforever.client.remote.domain.inbound.InboundMessage;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public abstract class GpgInboundMessage extends InboundMessage {

  private final List<Object> args;

  public GpgInboundMessage(List<Object> args) {
    super(MessageTarget.GAME);
    this.args = args;
  }

  protected GpgInboundMessage(int numberOfArgs) {
    this(new ArrayList<>(Collections.nCopies(numberOfArgs, null)));
  }

  protected void setArgAsValue(int index, Object value) {
    args.set(index, value);
  }

  protected int getArgAsInt(int index) {
    return ((Number) args.get(index)).intValue();
  }

  protected boolean getArgAsBoolean(int index) {
    return (boolean) args.get(index);
  }

  protected String getArgAsString(int index) {
    return ((String) args.get(index));
  }

  @SuppressWarnings("unchecked")
  protected <T> T getArgAsObject(int index) {
    return (T) args.get(index);
  }

}
