package com.faforever.client.notification;

import com.faforever.client.fx.Controller;
import com.faforever.client.preferences.PreferencesService;
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
public class TransientNotificationsController implements Controller<Node> {

  private final UiService uiService;
  private final PreferencesService preferencesService;
  public VBox transientNotificationsRoot;

  public void initialize() {
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
    TransientNotificationController controller = uiService.loadFxml("theme/transient_notification.fxml");
    controller.setNotification(notification);
    Region controllerRoot = controller.getRoot();
    transientNotificationsRoot.getChildren().add(0, controllerRoot);
  }
}
