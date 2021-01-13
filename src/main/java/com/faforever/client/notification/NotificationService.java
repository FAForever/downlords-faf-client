package com.faforever.client.notification;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.events.ImmediateErrorNotificationEvent;
import com.faforever.client.notification.events.ImmediateNotificationEvent;
import com.faforever.client.notification.events.PersistentNotificationEvent;
import com.faforever.client.notification.events.TransientNotificationEvent;
import com.faforever.client.reporting.ReportingService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.faforever.client.notification.Severity.ERROR;
import static javafx.collections.FXCollections.observableSet;
import static javafx.collections.FXCollections.synchronizedObservableSet;


@Lazy
@Service
@RequiredArgsConstructor
// TODO instead of being required an called explicitly, this service should listen for application events only
public class NotificationService implements InitializingBean {

  private final ObservableSet<PersistentNotification> persistentNotifications = synchronizedObservableSet(observableSet(new TreeSet<>()));
  private final List<OnTransientNotificationListener> onTransientNotificationListeners = new ArrayList<>();
  private final List<OnImmediateNotificationListener> onImmediateNotificationListeners = new ArrayList<>();
  private final ReportingService reportingService;
  private final I18n i18n;
  private final EventBus eventBus;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  /**
   * Adds a listener to be notified about added/removed {@link PersistentNotification}s
   */

  public void addPersistentNotificationListener(SetChangeListener<PersistentNotification> listener) {
    JavaFxUtil.addListener(persistentNotifications, listener);
  }

  /**
   * Adds a listener to be notified whenever a {@link TransientNotification} has been fired.
   */
  
  public void addTransientNotificationListener(OnTransientNotificationListener listener) {
    onTransientNotificationListeners.add(listener);
  }

  
  public Set<PersistentNotification> getPersistentNotifications() {
    return Collections.unmodifiableSet(persistentNotifications);
  }


  public void removeNotification(PersistentNotification notification) {
    persistentNotifications.remove(notification);
  }


  public void addImmediateNotificationListener(OnImmediateNotificationListener listener) {
    onImmediateNotificationListeners.add(listener);
  }

  @Subscribe
  public void onPersistentNotification(PersistentNotificationEvent event) {
    persistentNotifications.add(new PersistentNotification(event.getText(), event.getSeverity(), event.getActions()));
  }

  @Subscribe
  public void onTransientNotification(TransientNotificationEvent event) {
    TransientNotification notification = new TransientNotification(event.getTitle(), event.getText(), event.getImage(), event.getActionCallback());
    onTransientNotificationListeners.forEach(listener -> listener.onTransientNotification(notification));
  }

  @Subscribe
  public void onImmediateNotification(ImmediateNotificationEvent event) {
    ImmediateNotification notification = new ImmediateNotification(event.getTitle(), event.getText(), event.getSeverity(),
        event.getThrowable(), event.getActions(), event.getCustomUI());
    onImmediateNotificationListeners.forEach(listener -> listener.onImmediateNotification(notification));
  }

  @Subscribe
  public void onImmediateErrorNotification(ImmediateErrorNotificationEvent event) {
    ImmediateNotification notification = new ImmediateNotification(i18n.get("errorTitle"), i18n.get(event.getMessageKey(), event.getArgs()),
        ERROR, event.getThrowable(), Arrays.asList(new DismissAction(i18n), new ReportAction(i18n, reportingService, event.getThrowable())));
    onImmediateNotificationListeners.forEach(listener -> listener.onImmediateNotification(notification));
  }
}
