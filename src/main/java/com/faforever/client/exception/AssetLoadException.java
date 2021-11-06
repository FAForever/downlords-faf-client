package com.faforever.client.exception;


public class AssetLoadException extends MinorNotifiableException {

  public AssetLoadException(String message, Throwable cause, String i18nKey, Object... i18nArgs) {
    super(message, cause, i18nKey, i18nArgs);
  }
}
