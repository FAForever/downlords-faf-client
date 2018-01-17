package com.faforever.client.notification;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.notificationEvents.ShowImmediateErrorNotificationEvent;
import com.faforever.client.notification.notificationEvents.ShowImmediateNotificationEvent;
import com.faforever.client.notification.notificationEvents.ShowPersistentErrorNotificationEvent;
import com.faforever.client.notification.notificationEvents.ShowPersistentNotificationEvent;
import com.faforever.client.notification.notificationEvents.ShowTransientNotificationEvent;
import com.faforever.client.reporting.SupportService;
import javafx.application.Platform;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.faforever.client.notification.Severity.ERROR;
import static java.util.Collections.singletonList;
import static javafx.collections.FXCollections.observableSet;
import static javafx.collections.FXCollections.synchronizedObservableSet;


@Lazy
@Service
// TODO instead of being required an called explicitly, this service should listen for application events only
public class NotificationServiceImpl implements NotificationService {

  private final ObservableSet<PersistentNotification> persistentNotifications;
  private final List<OnTransientNotificationListener> onTransientNotificationListeners;
  private final List<OnImmediateNotificationListener> onImmediateNotificationListeners;
  private final SupportService supportService;
  private final I18n i18n;

  @Inject
  public NotificationServiceImpl(SupportService supportService, I18n i18n) {
    this.supportService = supportService;
    this.i18n = i18n;

    persistentNotifications = synchronizedObservableSet(observableSet(new TreeSet<>()));
    onTransientNotificationListeners = new ArrayList<>();
    onImmediateNotificationListeners = new ArrayList<>();
  }

  private void addNotification(PersistentNotification notification) {
    persistentNotifications.add(notification);
  }

  private void addNotification(TransientNotification notification) {
    onTransientNotificationListeners.forEach(listener -> listener.onTransientNotification(notification));
  }

  private void addNotification(ImmediateNotification notification) {
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


  private void addPersistentErrorNotification(Throwable throwable, String messageKey, Object... args) {
    addNotification(new PersistentNotification(i18n.get(messageKey, args), ERROR, singletonList(new ReportAction(i18n, supportService, throwable))));
  }


  private void addImmediateErrorNotification(Throwable throwable, String messageKey, Object... args) {
    addNotification(new ImmediateNotification(i18n.get("errorTitle"), i18n.get(messageKey, args), ERROR, throwable,
        Arrays.asList(new DismissAction(i18n), new ReportAction(i18n, supportService, throwable))));
  }

  @EventListener
  public void onImmediateNotification(ShowImmediateNotificationEvent event) {
    Platform.runLater(() -> addNotification(event.getNotification()));
  }

  @EventListener
  public void onPersistentNotification(ShowPersistentNotificationEvent event) {
    Platform.runLater(() -> addNotification(event.getNotification()));
  }

  @EventListener
  public void onTransientNotification(ShowTransientNotificationEvent event) {
    Platform.runLater(() -> addNotification(event.getNotification()));
  }

  @EventListener
  public void onPersistentErrorNotification(ShowPersistentErrorNotificationEvent event) {
    Platform.runLater(() -> addPersistentErrorNotification(event.getThrowable(), event.getMessageKey(), event.getObjectArgs()));
  }

  @EventListener
  public void onImmediateErrorNotification(ShowImmediateErrorNotificationEvent event) {
    Platform.runLater(() -> addPersistentErrorNotification(event.getThrowable(), event.getMessageKey(), event.getObjectArgs()));
  }

}
