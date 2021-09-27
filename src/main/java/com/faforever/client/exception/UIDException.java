package com.faforever.client.exception;

public class UIDException extends MajorNotifiableException {
  public UIDException(String message, Throwable cause, String i18nKey, Object... i18nArgs) {
    super(message, cause, i18nKey, i18nArgs);
  }
}
