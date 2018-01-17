package com.faforever.client.notification.notificationEvents;

import com.faforever.client.notification.TransientNotification;
import lombok.Data;

@Data
public class ShowTransientNotificationEvent {
  private final TransientNotification notification;
}
