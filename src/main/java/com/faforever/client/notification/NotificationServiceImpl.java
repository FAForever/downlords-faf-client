package com.faforever.client.notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationServiceImpl implements NotificationService {

  private List<OnBarNotificationListener> onBarNotificationListeners;
  private List<OnToastNotificationListener> onToastNotificationListeners;

  public NotificationServiceImpl() {
    onBarNotificationListeners = new ArrayList<>();
    onToastNotificationListeners = new ArrayList<>();
  }

  @Override
  public void addNotification(BarNotification notification) {
    onBarNotificationListeners.forEach(listener -> listener.onBarNotification(notification));
  }

  @Override
  public void addNotification(ToastNotification notification) {
    onToastNotificationListeners.forEach(listener -> listener.onToastNotification(notification));
  }

  @Override
  public void addBarNotificationListener(OnBarNotificationListener listener) {
    onBarNotificationListeners.add(listener);
  }

  @Override
  public void addToastNotificationListener(OnToastNotificationListener listener) {
    onToastNotificationListeners.add(listener);
  }
}
