package com.faforever.client.notification.events;

import lombok.Data;

@Data
public class ImmediateErrorNotificationEvent {
  private final Throwable throwable;
  private final String messageKey;
  private final Object[] args;

  public ImmediateErrorNotificationEvent(Throwable throwable, String messageKey, Object... args) {
    this.throwable = throwable;
    this.messageKey = messageKey;
    this.args = args;
  }
}
