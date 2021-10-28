package com.faforever.client.login;

public class NoRefreshTokenException extends RuntimeException {

  public NoRefreshTokenException(String message) {
    super(message);
  }
}
