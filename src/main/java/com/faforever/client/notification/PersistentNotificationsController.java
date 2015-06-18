package com.faforever.client.notification;

import com.faforever.client.util.JavaFxUtil;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  private Map<PersistentNotification, Node> notificationsToNode;

  public PersistentNotificationsController() {
    notificationsToNode = new HashMap<>();
  }

  @PostConstruct
  void postConstruct() {
    addNotifications(notificationService.getPersistentNotifications());
    notificationService.addPersistentNotificationListener(change -> {
      synchronized (change.getList()) {
        while (change.next()) {
          if (change.wasAdded()) {
            List<? extends PersistentNotification> addedNotifications = change.getAddedSubList();
            addNotifications(addedNotifications);
          } else {
            removeNotifications(change.getRemoved());
          }
        }
      }
    });
  }

  private void removeNotifications(List<? extends PersistentNotification> removedNotifications) {
    ObservableList<Node> children = persistentNotificationsRoot.getChildren();

    for (PersistentNotification removedNotification : removedNotifications) {
      children.remove(notificationsToNode.get(removedNotification));
    }

    if (children.isEmpty()) {
      children.setAll(noNotificationsLabel);
    }
  }

  private void addNotifications(List<? extends PersistentNotification> addedNotifications) {
    ArrayList<Node> notificationNodes = new ArrayList<>();

    for (PersistentNotification notification : addedNotifications) {
      Node node = notificationNodeFactory.createPersistentNotificationNode(notification);
      notificationNodes.add(node);
      notificationsToNode.put(notification, node);
    }

    Platform.runLater(() -> {
      ObservableList<Node> children = persistentNotificationsRoot.getChildren();
      children.remove(noNotificationsLabel);
      children.addAll(notificationNodes);
    });
  }

  public Node getRoot() {
    return persistentNotificationsRoot;
  }

}
