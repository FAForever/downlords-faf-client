package com.faforever.client.notification;

import com.faforever.client.exception.MajorNotifiableException;
import com.faforever.client.exception.MinorNotifiableException;
import com.faforever.client.exception.NotifiableException;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.reporting.ReportingService;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.faforever.client.notification.Severity.ERROR;
import static com.faforever.client.notification.Severity.WARN;
import static java.util.Collections.singletonList;
import static javafx.collections.FXCollections.observableSet;
import static javafx.collections.FXCollections.synchronizedObservableSet;


@Lazy
@Service
@RequiredArgsConstructor
// TODO instead of being required an called explicitly, this service should listen for application events only
public class NotificationService {

  private final ObservableSet<PersistentNotification> persistentNotifications = synchronizedObservableSet(observableSet(new TreeSet<>()));
  private final List<OnTransientNotificationListener> onTransientNotificationListeners = new ArrayList<>();
  private final List<OnImmediateNotificationListener> onImmediateNotificationListeners = new ArrayList<>();
  private final List<OnImmediateNotificationListener> onServerNotificationListeners = new ArrayList<>();
  private final ReportingService reportingService;
  private final I18n i18n;

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
   * Adds a {@link ImmediateNotification} to be displayed.
   */

  public void addServerNotification(ImmediateNotification notification) {
    onServerNotificationListeners.forEach(listener -> listener.onImmediateNotification(notification));
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

  public void addServerNotificationListener(OnImmediateNotificationListener listener) {
    onServerNotificationListeners.add(listener);
  }

  public void addPersistentErrorNotification(Throwable throwable, String messageKey, Object... args) {
    addNotification(new PersistentNotification(i18n.get(messageKey, args), ERROR, singletonList(new GetHelpAction(i18n, reportingService))));
  }

  public void addImmediateErrorNotification(Throwable throwable, String messageKey, Object... args) {
    addNotification(new ImmediateNotification(i18n.get("errorTitle"), i18n.get(messageKey, args), ERROR, throwable,
        Arrays.asList(new CopyErrorAction(i18n, reportingService, throwable), new GetHelpAction(i18n, reportingService), new DismissAction(i18n))));
  }

  public void addErrorNotification(NotifiableException throwable) {
    if (throwable instanceof MajorNotifiableException) {
      addImmediateErrorNotification(throwable.getCause(), throwable.getI18nKey(), throwable.getI18nArgs());
    } else if (throwable instanceof MinorNotifiableException) {
      addPersistentErrorNotification(throwable.getCause(), throwable.getI18nKey(), throwable.getI18nArgs());
    }
  }

  public void addImmediateWarnNotification(String messageKey, Object... args) {
    addNotification(new ImmediateNotification(i18n.get("errorTitle"), i18n.get(messageKey, args), WARN, List.of(new DismissAction(i18n))));
  }
}
