package com.faforever.client.remote.domain;

import com.google.gson.annotations.SerializedName;

public class RestoreGameSessionMessage extends ClientMessage {

  @SerializedName("game_id")
  private final int gameId;

  public RestoreGameSessionMessage(int gameId) {
    super(ClientMessageType.RESTORE_GAME_SESSION);
    this.gameId = gameId;
  }
}
