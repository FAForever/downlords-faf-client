package com.faforever.client.notification;

import com.faforever.client.fx.NodeController;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.preferences.ToastPosition;
import com.faforever.client.theme.UiService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class TransientNotificationsController extends NodeController<Node> {

  private final UiService uiService;
  private final NotificationPrefs notificationPrefs;

  public VBox transientNotificationsRoot;

  @Override
  protected void onInitialize() {
    ToastPosition toastPosition = notificationPrefs.getToastPosition();

    switch (toastPosition) {
      case TOP_RIGHT -> transientNotificationsRoot.setAlignment(Pos.TOP_RIGHT);
      case BOTTOM_RIGHT -> transientNotificationsRoot.setAlignment(Pos.BOTTOM_RIGHT);
      case BOTTOM_LEFT -> transientNotificationsRoot.setAlignment(Pos.BOTTOM_LEFT);
      case TOP_LEFT -> transientNotificationsRoot.setAlignment(Pos.TOP_LEFT);
      default -> throw new AssertionError("Uncovered position: " + toastPosition);
    }
  }

  @Override
  public Pane getRoot() {
    return transientNotificationsRoot;
  }

  public void addNotification(TransientNotification notification) {
    TransientNotificationController controller = uiService.loadFxml("theme/transient_notification.fxml");
    controller.setNotification(notification);
    Region controllerRoot = controller.getRoot();
    transientNotificationsRoot.getChildren().add(0, controllerRoot);
  }
}
