package com.faforever.client.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract sealed class NotifiableException extends RuntimeException permits MajorNotifiableException, MinorNotifiableException {
  String i18nKey;
  Object[] i18nArgs;

  public NotifiableException(String message, Throwable cause, String i18nKey, Object... i18nArgs) {
    super(message, cause);
    this.i18nKey = i18nKey;
    this.i18nArgs = i18nArgs;
  }
}
