package com.faforever.client.login;

public class NoRefreshTokenException extends TokenRetrievalException {

  public NoRefreshTokenException(String message) {
    super(message);
  }
}
