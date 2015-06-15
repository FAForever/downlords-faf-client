package com.faforever.client.notification;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static javafx.collections.FXCollections.observableArrayList;
import static javafx.collections.FXCollections.synchronizedObservableList;

public class NotificationServiceImpl implements NotificationService {

  private ObservableList<PersistentNotification> persistentNotifications;
  private List<OnTransientNotificationListener> onTransientNotificationListeners;

  public NotificationServiceImpl() {
    persistentNotifications = synchronizedObservableList(observableArrayList());
    onTransientNotificationListeners = new ArrayList<>();
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
  public void addPersistentNotificationListener(ListChangeListener<PersistentNotification> listener) {
    persistentNotifications.addListener(listener);
  }

  @Override
  public void addTransientNotificationListener(OnTransientNotificationListener listener) {
    onTransientNotificationListeners.add(listener);
  }

  @Override
  public List<PersistentNotification> getPersistentNotifications() {
    return Collections.unmodifiableList(persistentNotifications);
  }

  @Override
  public void removeNotification(PersistentNotification notification) {
    persistentNotifications.remove(notification);
  }
}
