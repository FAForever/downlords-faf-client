package com.faforever.client.notification;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;

public class ImmediateNotificationController {

  @FXML
  Label messageLabel;

  @FXML
  Label titleLabel;

  @FXML
  ButtonBar buttonBar;

  @FXML
  Node notificationRoot;

  public void setNotification(ImmediateNotification notification) {
    titleLabel.setText(notification.getTitle());
    messageLabel.setText(notification.getText());

    if (notification.getActions() != null) {
      for (Action action : notification.getActions()) {
        buttonBar.getButtons().add(createButton(action));
      }
    }
  }

  private Button createButton(Action action) {
    Button button = new Button(action.getTitle());
    button.setOnAction(event -> {
      action.call(event);
      if (action.getType() == Action.Type.OK_DONE) {
        dismiss();
      }
    });

    switch (action.getType()) {
      case OK_DONE:
        ButtonBar.setButtonData(button, ButtonBar.ButtonData.OK_DONE);
        break;
    }

    return button;
  }

  private void dismiss() {
    notificationRoot.getScene().getWindow().hide();
  }

  public Node getRoot() {
    return notificationRoot;
  }
}
