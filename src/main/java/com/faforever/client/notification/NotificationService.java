package com.faforever.client.notification;

import javafx.collections.SetChangeListener;

import java.util.Set;

public interface NotificationService {

  /**
   * Adds a {@link PersistentNotification} to be displayed.
   */
  void addNotification(PersistentNotification notification);

  /**
   * Adds a {@link TransientNotification} to be displayed.
   */
  void addNotification(TransientNotification notification);

  void addNotification(ImmediateNotification notification);

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

  void addPersistentErrorNotification(Throwable throwable, String messageKey, Object... args);

  // TODO refactor code to use this method where applicable
  void addImmediateErrorNotification(Throwable throwable, String messageKey, Object... args);
}
