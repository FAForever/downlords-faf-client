package com.faforever.client.remote.domain;

import lombok.Getter;

@Getter
public class MakeBroadcastMessage extends ClientMessage {
  final private String action = "broadcast";
  final private String message;

  public MakeBroadcastMessage(String message) {
    super(ClientMessageType.ADMIN);
    this.message = message;
  }
}