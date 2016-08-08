package com.faforever.client.mod;

public class ModLoadFailException extends RuntimeException {
  public ModLoadFailException(Throwable cause) {
    super(cause);
  }

  public ModLoadFailException(String message) {
    super(message);
  }
}
