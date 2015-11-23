package com.faforever.client.legacy.domain;

public class AuthenticationFailedMessage extends ServerMessage {

  private String text;

  public AuthenticationFailedMessage() {
    super(ServerMessageType.AUTHENTICATION_FAILED);
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
