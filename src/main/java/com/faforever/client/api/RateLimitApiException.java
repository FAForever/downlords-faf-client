package com.faforever.client.api;

import java.io.IOException;

public class RateLimitApiException extends IOException {

  public RateLimitApiException(Throwable cause) {
    super(cause);
  }
}
