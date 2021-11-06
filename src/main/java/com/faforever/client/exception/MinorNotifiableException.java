package com.faforever.client.exception;

public abstract class MinorNotifiableException extends NotifiableException {
  public MinorNotifiableException(String message, Throwable cause, String i18nKey, Object... i18nArgs) {
    super(message, cause, i18nKey, i18nArgs);
  }
}
