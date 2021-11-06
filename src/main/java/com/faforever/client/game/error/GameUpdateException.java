package com.faforever.client.game.error;

import com.faforever.client.exception.MinorNotifiableException;


public class GameUpdateException extends MinorNotifiableException {
  public GameUpdateException(String message, Throwable cause, String i18nKey, Object... i18nArgs) {
    super(message, cause, i18nKey, i18nArgs);
  }
}
