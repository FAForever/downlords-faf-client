package com.faforever.client.notification;

import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.preferences.PreferencesService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import org.springframework.beans.factory.annotation.Autowired;

public class NotificationNodeFactoryImpl implements NotificationNodeFactory {

  @FXML
  Node persistentNotificationsRoot;

  @Autowired
  FxmlLoader fxmlLoader;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  NotificationService notificationService;

  @Override
  public Node createPersistentNotificationNode(PersistentNotification notification) {
    /*
     * Since the controller is just one of many and temporary, it is not registered as a bean. So no autowiring
     * can be done.
     */
    PersistentNotificationController controller = fxmlLoader.loadAndGetController("persistent_notification.fxml");
    controller.setNotificationService(notificationService);
    controller.setTheme(preferencesService.getPreferences().getTheme());
    controller.setNotification(notification);
    return controller.getRoot();
  }

  @Override
  public Node createTransientNotificationPane(TransientNotification notification) {
    throw new UnsupportedOperationException("Not yet implemented");
  }
}
