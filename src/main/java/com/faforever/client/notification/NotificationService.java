package com.faforever.client.notification;

import javafx.collections.ListChangeListener;

import java.util.List;

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
  void addPersistentNotificationListener(ListChangeListener<PersistentNotification> listener);

  /**
   * Adds a listener to be notified whenever a {@link TransientNotification} has been fired.
   */
  void addTransientNotificationListener(OnTransientNotificationListener listener);

  List<PersistentNotification> getPersistentNotifications();

  void removeNotification(PersistentNotification notification);
}
