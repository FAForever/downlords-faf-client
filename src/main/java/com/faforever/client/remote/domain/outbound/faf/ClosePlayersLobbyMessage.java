package com.faforever.client.remote.domain.outbound.faf;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class ClosePlayersLobbyMessage extends AdminMessage {
  int userId;

  public ClosePlayersLobbyMessage(int userId) {
    super("closelobby");
    this.userId = userId;
  }
}