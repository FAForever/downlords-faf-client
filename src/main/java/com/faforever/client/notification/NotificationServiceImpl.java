package com.faforever.client.notification;

import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static javafx.collections.FXCollections.observableSet;
import static javafx.collections.FXCollections.synchronizedObservableSet;

public class NotificationServiceImpl implements NotificationService {

  private final ObservableSet<PersistentNotification> persistentNotifications;
  private final List<OnTransientNotificationListener> onTransientNotificationListeners;
  private final List<OnImmediateNotificationListener> onImmediateNotificationListeners;

  public NotificationServiceImpl() {
    persistentNotifications = synchronizedObservableSet(observableSet(new TreeSet<>()));
    onTransientNotificationListeners = new ArrayList<>();
    onImmediateNotificationListeners = new ArrayList<>();
  }

  @Override
  public void addNotification(PersistentNotification notification) {
    persistentNotifications.add(notification);
  }

  @Override
  public void addNotification(TransientNotification notification) {
    onTransientNotificationListeners.forEach(listener -> listener.onTransientNotification(notification));
  }

  @Override
  public void addNotification(ImmediateNotification notification) {
    onImmediateNotificationListeners.forEach(listener -> listener.onImmediateNotification(notification));
  }

  @Override
  public void addPersistentNotificationListener(SetChangeListener<PersistentNotification> listener) {
    persistentNotifications.addListener(listener);
  }

  @Override
  public void addTransientNotificationListener(OnTransientNotificationListener listener) {
    onTransientNotificationListeners.add(listener);
  }

  @Override
  public Set<PersistentNotification> getPersistentNotifications() {
    return Collections.unmodifiableSet(persistentNotifications);
  }

  @Override
  public void removeNotification(PersistentNotification notification) {
    persistentNotifications.remove(notification);
  }

  @Override
  public void addImmediateNotificationListener(OnImmediateNotificationListener listener) {
    onImmediateNotificationListeners.add(listener);
  }
}
