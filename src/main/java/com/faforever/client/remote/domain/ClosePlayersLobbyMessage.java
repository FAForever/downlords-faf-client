package com.faforever.client.remote.domain;

import lombok.Getter;

@Getter
public class ClosePlayersLobbyMessage extends ClientMessage {
  final private int user_id;
  final private String action = "closelobby";

  public ClosePlayersLobbyMessage(int user_id) {
    super(ClientMessageType.ADMIN);
    this.user_id = user_id;
  }
}