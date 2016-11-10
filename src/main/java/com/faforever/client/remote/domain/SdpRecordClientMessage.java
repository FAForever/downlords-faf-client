package com.faforever.client.remote.domain;

import com.faforever.client.fa.relay.GpgClientCommand;
import com.faforever.client.fa.relay.GpgGameMessage;

import java.util.Arrays;

public class SdpRecordClientMessage extends GpgGameMessage {
  private static final int RECEIVER_INDEX = 0;
  private static final int RECORD_INDEX = 1;

  public SdpRecordClientMessage(int receiver, String record) {
    super(GpgClientCommand.SDP_RECORD, Arrays.asList(receiver, record));
  }

  public int getReceiver() {
    return getInt(RECEIVER_INDEX);
  }

  public String getRecord() {
    return getString(RECORD_INDEX);
  }
}
