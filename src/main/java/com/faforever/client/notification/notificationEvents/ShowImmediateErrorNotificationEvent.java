package com.faforever.client.notification.notificationEvents;

import lombok.Data;

@Data
public class ShowImmediateErrorNotificationEvent {
  private final Throwable throwable;
  private final String messageKey;
  private final Object objectArgs[];

  public ShowImmediateErrorNotificationEvent(Throwable throwable, String messageKey, Object... objectArgs) {
    this.throwable = throwable;
    this.messageKey = messageKey;
    this.objectArgs = objectArgs;
  }
}
