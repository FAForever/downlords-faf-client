package com.faforever.client.remote.domain;

import com.faforever.client.fa.relay.GpgServerMessage;
import com.faforever.client.fa.relay.GpgServerMessageType;

import java.util.Map;

public class IceServerMessage extends GpgServerMessage {
  private static final int SENDER_INDEX = 0;
  private static final int RECORD_INDEX = 1;

  public IceServerMessage() {
    super(GpgServerMessageType.ICE_MESSAGE, 2);
  }

  public int getSender() {
    return getInt(SENDER_INDEX);
  }

  public Map<String, Object> getRecord() {
    return getObject(RECORD_INDEX);
  }
}
