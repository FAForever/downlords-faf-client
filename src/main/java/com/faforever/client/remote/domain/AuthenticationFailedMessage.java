package com.faforever.client.remote.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthenticationFailedMessage extends FafServerMessage {

  private String text;
  private String context;

  public AuthenticationFailedMessage() {
    super(FafServerMessageType.AUTHENTICATION_FAILED);
  }
}
