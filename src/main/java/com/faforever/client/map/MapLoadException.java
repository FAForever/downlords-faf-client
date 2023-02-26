package com.faforever.client.map;

import lombok.Getter;

@Getter
public class MapLoadException extends RuntimeException {
  private final String i18nKey;
  private final Object[] i18nArgs;

  public MapLoadException(String message, Throwable cause, String i18nKey, Object... i18nArgs) {
    super(message, cause);
    this.i18nKey = i18nKey;
    this.i18nArgs = i18nArgs;
  }
}
