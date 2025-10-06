package com.faforever.client.notification;

import com.faforever.client.exception.MajorNotifiableException;
import com.faforever.client.exception.MinorNotifiableException;
import com.faforever.client.exception.NotifiableException;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.i18n.I18n;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.ui.alert.Alert;
import com.faforever.client.ui.alert.animation.AlertAnimation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.Parent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.TreeSet;

import static com.faforever.client.notification.Severity.ERROR;
import static com.faforever.client.notification.Severity.INFO;
import static com.faforever.client.notification.Severity.WARN;
import static java.util.Collections.singletonList;
import static javafx.collections.FXCollections.observableSet;
import static javafx.collections.FXCollections.synchronizedObservableSet;


@Lazy
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final UiService uiService;
  private final ReportingService reportingService;
  private final I18n i18n;
  private final ToastDisplayer toastDisplayer;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final ObservableSet<PersistentNotification> persistentNotifications = synchronizedObservableSet(observableSet(new TreeSet<>()));

  public ObservableSet<PersistentNotification> getPersistentNotifications() {
    return FXCollections.unmodifiableObservableSet(persistentNotifications);
  }

  public void addNotification(Notification notification) {
    switch (notification) {
      case ImmediateNotification immediateNotification -> displayImmediateNotification(immediateNotification);
      case ServerNotification serverNotification -> displayServerNotification(serverNotification);
      case PersistentNotification persistentNotification -> persistentNotifications.add(persistentNotification);
      case TransientNotification transientNotification -> toastDisplayer.addNotification(transientNotification);
    }
  }

  public void removeNotification(Notification notification) {
    switch (notification) {
      case PersistentNotification persistentNotification -> persistentNotifications.remove(persistentNotification);
      case ImmediateNotification _ -> {}
      case ServerNotification _ -> {}
      case TransientNotification _ -> {}
    }
  }

  public void addPersistentErrorNotification(String messageKey, Object... args) {
    addNotification(new PersistentNotification(i18n.get(messageKey, args), ERROR, singletonList(new GetHelpAction(i18n, reportingService))));
  }

  public void addPersistentWarnNotification(List<Action> actions, String messageKey, Object... args) {
    addNotification(new PersistentNotification(i18n.get(messageKey, args), WARN, actions));
  }

  public void addImmediateErrorNotification(Throwable throwable, String messageKey, Object... args) {
    addNotification(new ImmediateNotification(i18n.get("errorTitle"), i18n.get(messageKey, args), ERROR, throwable,
        List.of(new DismissAction(i18n))));
  }

  public void addErrorNotification(NotifiableException throwable) {
    switch (throwable) {
      case MajorNotifiableException majorNotifiableException ->
          addImmediateErrorNotification(majorNotifiableException.getCause(), majorNotifiableException.getI18nKey(),
                                        majorNotifiableException.getI18nArgs());
      case MinorNotifiableException minorNotifiableException ->
          addPersistentErrorNotification(minorNotifiableException.getI18nKey(), minorNotifiableException.getI18nArgs());
    }
  }

  public void addImmediateWarnNotification(String messageKey, Object... args) {
    addNotification(new ImmediateNotification(i18n.get("errorTitle"), i18n.get(messageKey, args), WARN, List.of(new DismissAction(i18n))));
  }

  public void addImmediateWarnNotification(String title, String text, List<Action> actions, Parent customUI) {
    addNotification(new ImmediateNotification(title, text, WARN, actions, customUI));
  }

  public void addImmediateInfoNotification(String messageKey, Object... args) {
    addNotification(new ImmediateNotification("", i18n.get(messageKey, args), INFO, List.of(new OkAction(i18n))));
  }

  private void displayImmediateNotification(ImmediateNotification notification) {
    ImmediateNotificationController controller = uiService.loadFxml("theme/immediate_notification.fxml");

    controller.setNotification(notification);

    fxApplicationThreadExecutor.execute(() -> {
      Alert<?> dialog = new Alert<>(StageHolder.getStage());

      controller.setCloseListener(dialog::close);

      dialog.setContent(controller.getDialogLayout());
      dialog.setAnimation(AlertAnimation.TOP_ANIMATION);
      dialog.show();
    });
  }

  private void displayServerNotification(ServerNotification notification) {
    fxApplicationThreadExecutor.execute(() -> {
      ServerNotificationController controller = uiService.loadFxml("theme/server_notification.fxml");
      controller.setNotification(notification);

      Alert<?> dialog = new Alert<>(StageHolder.getStage());

      controller.setCloseListener(dialog::close);

      dialog.setContent(controller.getDialogLayout());
      dialog.setAnimation(AlertAnimation.TOP_ANIMATION);
      dialog.show();
    });
  }
}
