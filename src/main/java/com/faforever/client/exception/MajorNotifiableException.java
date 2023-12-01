package com.faforever.client.exception;

public abstract non-sealed class MajorNotifiableException extends NotifiableException {
  public MajorNotifiableException(String message, Throwable cause, String i18nKey, Object... i18nArgs) {
    super(message, cause, i18nKey, i18nArgs);
  }
}
