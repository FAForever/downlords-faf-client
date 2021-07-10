package com.faforever.client.remote.domain.inbound.gpg;

import com.faforever.client.remote.domain.MessageTarget;
import com.faforever.client.remote.domain.inbound.InboundMessage;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

  @JsonIgnore
  protected void setArgAsValue(int index, Object value) {
    args.set(index, value);
  }

  @JsonIgnore
  protected int getArgAsInt(int index) {
    return ((Number) args.get(index)).intValue();
  }

  @JsonIgnore
  protected boolean getArgAsBoolean(int index) {
    return (boolean) args.get(index);
  }

  @JsonIgnore
  protected String getArgAsString(int index) {
    return ((String) args.get(index));
  }

  @JsonIgnore
  @SuppressWarnings("unchecked")
  protected <T> T getArgAsObject(int index) {
    return (T) args.get(index);
  }

}
