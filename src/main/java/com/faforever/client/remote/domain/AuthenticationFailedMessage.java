package com.faforever.client.remote.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthenticationFailedMessage extends FafServerMessage {

  private String text;
  private String context;
  // Policy server result when context == "policy"
  private String result;

  public AuthenticationFailedMessage() {
    super(FafServerMessageType.AUTHENTICATION_FAILED);
  }
}
