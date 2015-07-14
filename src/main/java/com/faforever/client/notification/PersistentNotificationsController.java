package com.faforever.client.notification;

import com.faforever.client.sound.SoundController;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Controller for pane that displays all persistent notifications.
 */
public class PersistentNotificationsController {

  @Autowired
  ApplicationContext applicationContext;

  @FXML
  Label noNotificationsLabel;

  @FXML
  Pane persistentNotificationsRoot;

  @Autowired
  NotificationService notificationService;

  @Autowired
  SoundController soundController;

  private Map<PersistentNotification, Node> notificationsToNode;

  public PersistentNotificationsController() {
    notificationsToNode = new HashMap<>();
  }

  @PostConstruct
  void postConstruct() {
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

  private void removeNotification(PersistentNotification removedNotifications) {
    ObservableList<Node> children = persistentNotificationsRoot.getChildren();
    children.remove(notificationsToNode.get(removedNotifications));

    if (children.isEmpty()) {
      children.setAll(noNotificationsLabel);
    }
  }

  private void addNotifications(Set<PersistentNotification> persistentNotifications) {
    for (PersistentNotification persistentNotification : persistentNotifications) {
      addNotification(persistentNotification);
    }
  }

  private void addNotification(PersistentNotification notification) {
    PersistentNotificationController controller = applicationContext.getBean(PersistentNotificationController.class);
    controller.setNotification(notification);

    notificationsToNode.put(notification, controller.getRoot());

    Platform.runLater(() -> {
      ObservableList<Node> children = persistentNotificationsRoot.getChildren();
      children.remove(noNotificationsLabel);
      children.add(controller.getRoot());

      playNotificationSound(notification);
    });
  }

  private void playNotificationSound(PersistentNotification notification) {
    switch (notification.getSeverity()) {
      case INFO:
        soundController.playInfoNotificationSound();
        break;

      case WARN:
        soundController.playWarnNotificationSound();
        break;

      case ERROR:
        soundController.playErrorNotificationSound();
        break;
    }
  }

  public Node getRoot() {
    return persistentNotificationsRoot;
  }

}
