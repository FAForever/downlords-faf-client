package com.faforever.client.notification;

import com.faforever.client.audio.AudioService;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.NodeController;
import com.faforever.client.theme.UiService;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for pane that displays all persistent notifications.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class PersistentNotificationsController extends NodeController<Node> {

  private final Map<PersistentNotification, Node> notificationsToNode = new HashMap<>();
  private final NotificationService notificationService;
  private final AudioService audioService;
  private final UiService uiService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public Label noNotificationsLabel;
  public Pane persistentNotificationsRoot;

  @Override
  protected void onInitialize() {
    notificationService.getPersistentNotifications().addListener((SetChangeListener<PersistentNotification>) change -> {
      if (change.wasAdded()) {
        PersistentNotification addedNotifications = change.getElementAdded();
        addNotification(addedNotifications);
      } else {
        removeNotification(change.getElementRemoved());
      }
    });
    notificationService.getPersistentNotifications().forEach(this::addNotification);
  }

  private void addNotification(PersistentNotification notification) {
    PersistentNotificationController controller = uiService.loadFxml("theme/persistent_notification.fxml");
    controller.setNotification(notification);

    notificationsToNode.put(notification, controller.getRoot());

    fxApplicationThreadExecutor.execute(() -> {
      ObservableList<Node> children = persistentNotificationsRoot.getChildren();
      children.remove(noNotificationsLabel);
      children.add(controller.getRoot());

      playNotificationSound(notification);
    });
  }

  private void removeNotification(PersistentNotification removedNotifications) {
    ObservableList<Node> children = persistentNotificationsRoot.getChildren();
    children.remove(notificationsToNode.get(removedNotifications));

    if (children.isEmpty()) {
      children.setAll(noNotificationsLabel);
    }
  }

  private void playNotificationSound(PersistentNotification notification) {
    switch (notification.severity()) {
      case INFO -> audioService.playInfoNotificationSound();
      case WARN -> audioService.playWarnNotificationSound();
      case ERROR -> audioService.playErrorNotificationSound();
    }
  }

  @Override
  public Node getRoot() {
    return persistentNotificationsRoot;
  }

}
