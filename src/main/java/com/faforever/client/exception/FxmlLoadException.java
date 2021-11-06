package com.faforever.client.exception;

public class FxmlLoadException extends MinorNotifiableException {
  public FxmlLoadException(String message, Throwable cause, String i18nKey, Object... i18nArgs) {
    super(message, cause, i18nKey, i18nArgs);
  }
}
