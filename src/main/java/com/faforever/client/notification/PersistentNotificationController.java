package com.faforever.client.notification;

import com.faforever.client.util.ThemeUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for a single persistent notification entry.
 */
public class PersistentNotificationController {

  @FXML
  Node notificationRoot;

  @FXML
  Label messageLabel;

  @FXML
  ImageView imageView;

  @FXML
  HBox actionButtonsContainer;

  private String theme;
  private NotificationService notificationService;
  private PersistentNotification notification;

  public void setNotification(PersistentNotification notification) {
    this.notification = notification;
    messageLabel.setText(notification.getText());
    setImageBasedOnSeverity(notification.getSeverity());
    setActions(notification.getActions());
  }

  private void setImageBasedOnSeverity(Severity severity) {
    if (theme == null) {
      throw new IllegalStateException("Theme must be set first");
    }

    Image image;
    switch (severity) {
      case INFO:
        image = new Image(ThemeUtil.themeFile(theme, "images/info.png"));
        break;
      case WARN:
        image = new Image(ThemeUtil.themeFile(theme, "images/warn.png"));
        break;
      case ERROR:
        image = new Image(ThemeUtil.themeFile(theme, "images/error.png"));
        break;
      default:
        throw new IllegalStateException("Unhandled severity: " + severity);
    }
    imageView.setImage(image);
  }

  private void setActions(List<Action> actions) {
    List<Button> actionButtons = new ArrayList<>();
    for (Action action : actions) {
      Button button = new Button(action.getTitle());
      button.setFocusTraversable(false);
      button.setOnAction(event -> {
        action.call(event);
        dismiss();
      });

      actionButtons.add(button);
    }

    actionButtonsContainer.getChildren().setAll(actionButtons);
  }

  private void dismiss() {
    notificationService.removeNotification(notification);
  }

  public Node getRoot() {
    return notificationRoot;
  }

  public void setTheme(String theme) {
    this.theme = theme;
  }

  public void onCloseButtonClicked(ActionEvent event) {
    dismiss();
  }

  public void setNotificationService(NotificationService notificationService) {
    this.notificationService = notificationService;
  }
}
