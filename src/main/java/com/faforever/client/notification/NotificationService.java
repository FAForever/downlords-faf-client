package com.faforever.client.notification;

public interface NotificationService {

  /**
   * Adds a {@link PersistentNotification} to be displayed.
   */
  void addNotification(PersistentNotification notification);

  /**
   * Adds a {@link TransientNotification} to be displayed.
   */
  void addNotification(TransientNotification notification);

  /**
   * Adds a listener to be notified about added/removed {@link PersistentNotification}s
   */
  void addBarNotificationListener(OnBarNotificationListener listener);

  /**
   * Adds a listener to be notified about added/removed {@link TransientNotification}s
   */
  void addToastNotificationListener(OnToastNotificationListener listener);
}
