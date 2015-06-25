package com.faforever.client.notification;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for a single persistent notification entry.
 */
public class PersistentNotificationController {

  private static final String CSS_STYLE_INFO = "info";
  private static final String CSS_STYLE_WARN = "warn";
  private static final String CSS_STYLE_ERROR = "error";

  @FXML
  Node notificationRoot;

  @FXML
  Label messageLabel;

  @FXML
  Label iconLabel;

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

    ObservableList<String> styleClasses = iconLabel.getStyleClass();
    styleClasses.removeAll(CSS_STYLE_INFO, CSS_STYLE_WARN, CSS_STYLE_ERROR);

    Image image;
    switch (severity) {
      case INFO:
        iconLabel.setText("\uf05a");
        styleClasses.add(CSS_STYLE_INFO);
        break;
      case WARN:
        iconLabel.setText("\uf071");
        styleClasses.add(CSS_STYLE_WARN);
        break;
      case ERROR:
        iconLabel.setText("\uf06a");
        styleClasses.add(CSS_STYLE_ERROR);
        break;
      default:
        throw new IllegalStateException("Unhandled severity: " + severity);
    }
    iconLabel.setLabelFor(iconLabel);
  }

  private void setActions(List<Action> actions) {
    if (actions == null) {
      return;
    }

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
