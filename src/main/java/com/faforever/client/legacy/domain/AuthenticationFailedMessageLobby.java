package com.faforever.client.legacy.domain;

public class AuthenticationFailedMessageLobby extends FafServerMessage {

  private String text;

  public AuthenticationFailedMessageLobby() {
    super(FafServerMessageType.AUTHENTICATION_FAILED);
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
