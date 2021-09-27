package com.faforever.client.mod;

import lombok.Getter;

@Getter
public class ModLoadException extends Exception {
  private final String i18nKey;
  private final Object[] i18nArgs;

  public ModLoadException(String message, Throwable cause, String i18nKey, Object... i18nArgs) {
    super(message, cause);
    this.i18nKey = i18nKey;
    this.i18nArgs = i18nArgs;
  }
}
