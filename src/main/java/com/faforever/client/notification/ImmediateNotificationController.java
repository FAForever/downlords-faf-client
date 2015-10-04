package com.faforever.client.notification;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ImmediateNotificationController {

  @FXML
  TextArea exceptionTextArea;

  @FXML
  Label messageLabel;

  @FXML
  Label titleLabel;

  @FXML
  ButtonBar buttonBar;

  @FXML
  Region notificationRoot;

  public void setNotification(ImmediateNotification notification) {
    titleLabel.setText(notification.getTitle());
    messageLabel.setText(notification.getText());

    if (notification.getThrowable() != null) {
      StringWriter writer = new StringWriter();
      notification.getThrowable().printStackTrace(new PrintWriter(writer));
      exceptionTextArea.setText(writer.toString());
    }

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

  public Region getRoot() {
    return notificationRoot;
  }
}
