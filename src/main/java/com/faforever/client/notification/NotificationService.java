package com.faforever.client.notification;

import javafx.collections.SetChangeListener;

import java.util.Set;

public interface NotificationService {
  /**
   * Adds a listener to be notified about added/removed {@link PersistentNotification}s
   */
  void addPersistentNotificationListener(SetChangeListener<PersistentNotification> listener);

  /**
   * Adds a listener to be notified whenever a {@link TransientNotification} has been fired.
   */
  void addTransientNotificationListener(OnTransientNotificationListener listener);

  Set<PersistentNotification> getPersistentNotifications();

  void removeNotification(PersistentNotification notification);

  void addImmediateNotificationListener(OnImmediateNotificationListener listener);
}
