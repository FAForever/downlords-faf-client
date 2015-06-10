package com.faforever.client.notification;

public interface NotificationService {

  /**
   * Adds a {@link BarNotification} to be displayed.
   */
  void addNotification(BarNotification notification);

  /**
   * Adds a {@link ToastNotification} to be displayed.
   */
  void addNotification(ToastNotification notification);

  /**
   * Adds a listener to be notified about added/removed {@link BarNotification}s
   */
  void addBarNotificationListener(OnBarNotificationListener listener);

  /**
   * Adds a listener to be notified about added/removed {@link ToastNotification}s
   */
  void addToastNotificationListener(OnToastNotificationListener listener);
}
