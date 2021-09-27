package com.faforever.client.game.error;

import com.faforever.client.exception.MajorNotifiableException;


public class GameLaunchException extends MajorNotifiableException {
  public GameLaunchException(String message, Throwable cause, String i18nKey, Object... i18nArgs) {
    super(message, cause, i18nKey, i18nArgs);
  }
}
