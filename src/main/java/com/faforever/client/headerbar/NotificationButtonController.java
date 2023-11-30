package com.faforever.client.headerbar;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.NodeController;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class NotificationButtonController extends NodeController<Node> {

  private static final PseudoClass NOTIFICATION_INFO_PSEUDO_CLASS = PseudoClass.getPseudoClass("info");
  private static final PseudoClass NOTIFICATION_WARN_PSEUDO_CLASS = PseudoClass.getPseudoClass("warn");
  private static final PseudoClass NOTIFICATION_ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error");

  private final NotificationService notificationService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public StackPane root;
  public Button notificationButton;
  public Popup persistentNotificationsPopup;

  @Override
  protected void onInitialize() {
    updateNotificationsButton(notificationService.getPersistentNotifications());
    notificationService.addPersistentNotificationListener(change -> fxApplicationThreadExecutor.execute(() -> updateNotificationsButton(change.getSet())));

    notificationButton.managedProperty().bind(notificationButton.visibleProperty());
  }

  /**
   * Updates the number displayed in the notifications button and sets its CSS pseudo class based on the highest
   * notification {@code Severity} of all current notifications.
   */
  private void updateNotificationsButton(Collection<? extends PersistentNotification> notifications) {
    int size = notifications.size();

    Severity highestSeverity = notifications.stream()
        .map(PersistentNotification::getSeverity)
        .max(Enum::compareTo)
        .orElse(null);

    fxApplicationThreadExecutor.execute(() -> {
      notificationButton.setVisible(size != 0);
      notificationButton.pseudoClassStateChanged(NOTIFICATION_INFO_PSEUDO_CLASS, highestSeverity == Severity.INFO);
      notificationButton.pseudoClassStateChanged(NOTIFICATION_WARN_PSEUDO_CLASS, highestSeverity == Severity.WARN);
      notificationButton.pseudoClassStateChanged(NOTIFICATION_ERROR_PSEUDO_CLASS, highestSeverity == Severity.ERROR);
    });
  }

  public void onNotificationsButtonClicked() {
    Bounds screenBounds = notificationButton.localToScreen(notificationButton.getBoundsInLocal());
    persistentNotificationsPopup.show(notificationButton.getScene()
        .getWindow(), screenBounds.getMaxX(), screenBounds.getMaxY());
  }

  @Override
  public StackPane getRoot() {
    return root;
  }
}
