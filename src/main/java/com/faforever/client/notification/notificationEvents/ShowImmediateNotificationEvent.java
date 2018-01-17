package com.faforever.client.notification.notificationEvents;

import com.faforever.client.notification.ImmediateNotification;
import lombok.Data;

@Data
public class ShowImmediateNotificationEvent {
  private final ImmediateNotification notification;
}
