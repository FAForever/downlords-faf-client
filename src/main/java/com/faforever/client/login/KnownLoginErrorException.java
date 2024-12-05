package com.faforever.client.login;

import lombok.Getter;

@Getter
public class KnownLoginErrorException extends RuntimeException {
  private final String i18nKey;
  
  public KnownLoginErrorException(String message, String i18nKey) {
    super(message);
    this.i18nKey = i18nKey;
  }
}
