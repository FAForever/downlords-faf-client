package com.faforever.client.net;

import java.net.URI;
import java.net.URISyntaxException;

public final class UriUtil {

  private UriUtil() {
    throw new AssertionError("Not instantiatable");
  }

  public static URI fromString(String string) {
    try {
      return new URI(string);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
