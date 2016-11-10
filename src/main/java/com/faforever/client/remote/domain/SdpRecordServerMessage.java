package com.faforever.client.remote.domain;

import com.faforever.client.fa.relay.GpgServerMessage;
import com.faforever.client.fa.relay.GpgServerMessageType;

public class SdpRecordServerMessage extends GpgServerMessage {
  private static final int SENDER_INDEX = 0;
  private static final int RECORD_INDEX = 1;

  public SdpRecordServerMessage() {
    super(GpgServerMessageType.SDP_RECORD, 2);
  }

  public int getSender() {
    return getInt(SENDER_INDEX);
  }

  public String getRecord() {
    return getString(RECORD_INDEX);
  }
}
