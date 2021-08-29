package com.faforever.client.login;

public class NoRedirectUriException extends RuntimeException {
  public NoRedirectUriException(String message) {
    super(message);
  }
}
