package com.faforever.client.remote.domain;

public class AuthenticationFailedMessage extends FafServerMessage {

  private String text;

  public AuthenticationFailedMessage() {
    super(FafServerMessageType.AUTHENTICATION_FAILED);
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
