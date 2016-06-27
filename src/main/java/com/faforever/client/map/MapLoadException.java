package com.faforever.client.map;

public class MapLoadException extends RuntimeException {

  public MapLoadException(Throwable cause) {
    super(cause);
  }

  public MapLoadException(String message) {
    super(message);
  }
}
