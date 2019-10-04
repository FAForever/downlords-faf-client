package com.faforever.client.notification;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.reporting.ReportingService;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import org.springframework.context.annotation.Lazy;
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
public class NotificationService {

  private final ObservableSet<PersistentNotification> persistentNotifications;
  private final List<OnTransientNotificationListener> onTransientNotificationListeners;
  private final List<OnImmediateNotificationListener> onImmediateNotificationListeners;
  private final ReportingService reportingService;

  // TODO fix circular reference

  private I18n i18n;


  public NotificationService(ReportingService reportingService) {
    this.reportingService = reportingService;

    persistentNotifications = synchronizedObservableSet(observableSet(new TreeSet<>()));
    onTransientNotificationListeners = new ArrayList<>();
    onImmediateNotificationListeners = new ArrayList<>();
  }

  /**
   * Adds a {@link PersistentNotification} to be displayed.
   */
  
  public void addNotification(PersistentNotification notification) {
    persistentNotifications.add(notification);
  }

  /**
   * Adds a {@link TransientNotification} to be displayed.
   */
  
  public void addNotification(TransientNotification notification) {
    onTransientNotificationListeners.forEach(listener -> listener.onTransientNotification(notification));
  }

  /**
   * Adds a {@link ImmediateNotification} to be displayed.
   */
  
  public void addNotification(ImmediateNotification notification) {
    onImmediateNotificationListeners.forEach(listener -> listener.onImmediateNotification(notification));
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

  
  public void addPersistentErrorNotification(Throwable throwable, String messageKey, Object... args) {
    addNotification(new PersistentNotification(i18n.get(messageKey, args), ERROR, singletonList(new ReportAction(i18n, reportingService, throwable))));
  }

  // TODO refactor code to use this method where applicable
  
  public void addImmediateErrorNotification(Throwable throwable, String messageKey, Object... args) {
    addNotification(new ImmediateNotification(i18n.get("errorTitle"), i18n.get(messageKey, args), ERROR, throwable,
        Arrays.asList(new DismissAction(i18n), new ReportAction(i18n, reportingService, throwable))));
  }
}
