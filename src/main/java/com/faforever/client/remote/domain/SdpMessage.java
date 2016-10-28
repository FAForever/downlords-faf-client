package com.faforever.client.remote.domain;

public class SdpMessage extends ClientMessage {
  private final int receiverId;
  private final String record;

  public SdpMessage(int receiverId, String record) {
    super(ClientMessageType.SDP);
    this.receiverId = receiverId;
    this.record = record;
  }
}
