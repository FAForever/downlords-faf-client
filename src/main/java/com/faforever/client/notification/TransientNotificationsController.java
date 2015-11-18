package com.faforever.client.notification;

import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.ToastPosition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.TreeSet;

public class TransientNotificationsController {

  private final TreeSet<TransientNotification> transientNotificationBuffer;
  @FXML
  VBox transientNotificationsRoot;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  PreferencesService preferencesService;

  public TransientNotificationsController() {
    transientNotificationBuffer = new TreeSet<>();
  }


  @PostConstruct
  void postConstruct() {
    ToastPosition toastPosition = preferencesService.getPreferences().getNotification().getToastPosition();

    switch (toastPosition) {
      case TOP_RIGHT:
        transientNotificationsRoot.setAlignment(Pos.TOP_RIGHT);
        break;
      case BOTTOM_RIGHT:
        transientNotificationsRoot.setAlignment(Pos.BOTTOM_RIGHT);
        break;
      case BOTTOM_LEFT:
        transientNotificationsRoot.setAlignment(Pos.BOTTOM_LEFT);
        break;
      case TOP_LEFT:
        transientNotificationsRoot.setAlignment(Pos.TOP_LEFT);
        break;
      default:
        throw new AssertionError("Uncovered position: " + toastPosition);
    }
  }

  public Pane getRoot() {
    return transientNotificationsRoot;
  }

  public void addNotification(TransientNotification notification) {
    TransientNotificationController controller = applicationContext.getBean(TransientNotificationController.class);
    controller.setNotification(notification);
    Region controllerRoot = controller.getRoot();
    transientNotificationsRoot.getChildren().add(0, controllerRoot);
  }
}
