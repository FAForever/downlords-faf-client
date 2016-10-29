package com.faforever.client.remote.domain;

public class SdpClientMessage extends ClientMessage {
  private final int receiver;
  private final String record;

  public SdpClientMessage(int receiver, String record) {
    super(ClientMessageType.SDP);
    this.receiver = receiver;
    this.record = record;
  }
}
