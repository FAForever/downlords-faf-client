package com.faforever.client.notification;

import com.faforever.client.audio.AudioController;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Controller for pane that displays all persistent notifications.
 */
public class PersistentNotificationsController {

  private final Map<PersistentNotification, Node> notificationsToNode;
  @FXML
  Label noNotificationsLabel;
  @FXML
  Pane persistentNotificationsRoot;
  @Resource
  NotificationService notificationService;
  @Resource
  AudioController audioController;
  @Resource
  ApplicationContext applicationContext;

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

  private void addNotifications(Set<PersistentNotification> persistentNotifications) {
    persistentNotifications.forEach(this::addNotification);
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
        audioController.playInfoNotificationSound();
        break;

      case WARN:
        audioController.playWarnNotificationSound();
        break;

      case ERROR:
        audioController.playErrorNotificationSound();
        break;
    }
  }

  public Node getRoot() {
    return persistentNotificationsRoot;
  }

}
