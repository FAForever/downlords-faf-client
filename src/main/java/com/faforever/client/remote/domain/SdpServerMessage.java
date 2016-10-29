package com.faforever.client.remote.domain;

public class SdpServerMessage extends FafServerMessage {
  private final int sender;
  private final String record;

  public SdpServerMessage(int sender, String record) {
    super(FafServerMessageType.SDP);
    this.sender = sender;
    this.record = record;
  }

  public int getSender() {
    return sender;
  }

  public String getRecord() {
    return record;
  }
}
