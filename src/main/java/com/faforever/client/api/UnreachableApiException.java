package com.faforever.client.api;

import java.io.IOException;

public class UnreachableApiException extends IOException {

  public UnreachableApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
