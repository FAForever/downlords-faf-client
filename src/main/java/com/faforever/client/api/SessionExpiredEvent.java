package com.faforever.client.api;

public class SessionExpiredEvent extends RuntimeException{
  public SessionExpiredEvent(Throwable cause) {
    super(cause);
  }
}
