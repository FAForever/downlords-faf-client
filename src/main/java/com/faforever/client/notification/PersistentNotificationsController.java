package com.faforever.client.notification;

import com.faforever.client.sound.SoundService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Controller for pane that displays all persistent notifications.
 */
public class PersistentNotificationsController {

  @FXML
  Label noNotificationsLabel;

  @FXML
  Pane persistentNotificationsRoot;

  @Autowired
  NotificationService notificationService;

  @Autowired
  NotificationNodeFactory notificationNodeFactory;

  @Autowired
  SoundService soundService;

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
    Node node = notificationNodeFactory.createPersistentNotificationNode(notification);
    notificationsToNode.put(notification, node);

    Platform.runLater(() -> {
      ObservableList<Node> children = persistentNotificationsRoot.getChildren();
      children.remove(noNotificationsLabel);
      children.add(node);

      playNotificationSound(notification);
    });
  }

  private void playNotificationSound(PersistentNotification notification) {
    switch (notification.getSeverity()) {
      case INFO:
        soundService.playInfoNotificationSound();
        break;

      case WARN:
        soundService.playWarnNotificationSound();
        break;

      case ERROR:
        soundService.playErrorNotificationSound();
        break;
    }
  }

  public Node getRoot() {
    return persistentNotificationsRoot;
  }

}
