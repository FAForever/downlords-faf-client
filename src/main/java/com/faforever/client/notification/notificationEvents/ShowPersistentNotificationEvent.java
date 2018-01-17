package com.faforever.client.notification.notificationEvents;

import com.faforever.client.notification.PersistentNotification;
import lombok.Data;

@Data
public class ShowPersistentNotificationEvent {
  private final PersistentNotification notification;
}
