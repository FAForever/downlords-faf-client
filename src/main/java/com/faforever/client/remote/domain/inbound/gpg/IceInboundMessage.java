package com.faforever.client.remote.domain.inbound.gpg;


import com.fasterxml.jackson.annotation.JsonIgnore;

public class IceInboundMessage extends GpgInboundMessage {
  public static final String COMMAND = "IceMsg";
  private static final int SENDER_INDEX = 0;
  private static final int RECORD_INDEX = 1;

  public IceInboundMessage() {
    super(2);
  }

  @JsonIgnore
  public int getSender() {
    return getArgAsInt(SENDER_INDEX);
  }

  @JsonIgnore
  public Object getRecord() {
    return getArgAsObject(RECORD_INDEX);
  }

  @JsonIgnore
  public void setSender(Integer sender) {
    setArgAsValue(SENDER_INDEX, sender);
  }

  @JsonIgnore
  public void setRecord(Object record) {
    setArgAsValue(RECORD_INDEX, record);
  }
}
