package com.faforever.client.remote.domain;

public class LoginMessage extends FafServerMessage {

  private PlayerInfo me;

  public LoginMessage() {
    super(FafServerMessageType.WELCOME);
  }

  public PlayerInfo getMe() {
    return me;
  }

  public void setMe(PlayerInfo me) {
    this.me = me;
  }
}
