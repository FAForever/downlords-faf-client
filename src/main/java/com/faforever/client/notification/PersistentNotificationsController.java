package com.faforever.client.notification;

import com.faforever.client.audio.AudioService;
import com.faforever.client.fx.Controller;
import com.faforever.client.theme.UiService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Controller for pane that displays all persistent notifications.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PersistentNotificationsController implements Controller<Node> {

  private final Map<PersistentNotification, Node> notificationsToNode;
  private final NotificationService notificationService;
  private final AudioService audioService;
  private final UiService uiService;
  public Label noNotificationsLabel;
  public Pane persistentNotificationsRoot;


  public PersistentNotificationsController(NotificationService notificationService, AudioService audioService, UiService uiService) {
    this.notificationService = notificationService;
    this.audioService = audioService;
    this.uiService = uiService;

    notificationsToNode = new HashMap<>();
  }

  public void initialize() {
    addNotifications(notificationService.getPersistentNotifications());
    notificationService.addPersistentNotificationListener(change -> {
      if (change.wasAdded()) {
        PersistentNotification addedNotifications = change.getElementAdded();
        addNotification(addedNotifications);
      } else {
        removeNotification(change.getElementRemoved());
      }
    });
  }

  private void addNotifications(Set<PersistentNotification> persistentNotifications) {
    persistentNotifications.forEach(this::addNotification);
  }

  private void addNotification(PersistentNotification notification) {
    PersistentNotificationController controller = uiService.loadFxml("theme/persistent_notification.fxml");
    controller.setNotification(notification);

    notificationsToNode.put(notification, controller.getRoot());

    Platform.runLater(() -> {
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
    switch (notification.getSeverity()) {
      case INFO:
        audioService.playInfoNotificationSound();
        break;

      case WARN:
        audioService.playWarnNotificationSound();
        break;

      case ERROR:
        audioService.playErrorNotificationSound();
        break;
    }
  }

  public Node getRoot() {
    return persistentNotificationsRoot;
  }

}
