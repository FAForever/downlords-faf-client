package com.faforever.client.remote.domain;

import lombok.Getter;

@Getter
public class ClosePlayersFAMessage extends ClientMessage {
  final private int user_id;
  final private String action = "closeFA";

  public ClosePlayersFAMessage(int user_id) {
    super(ClientMessageType.ADMIN);
    this.user_id = user_id;
  }
}