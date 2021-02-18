package com.faforever.client.notification;

import com.faforever.client.fx.Controller;
import com.faforever.client.ui.dialog.DialogLayout;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ImmediateNotificationController implements Controller<Node> {

  private final DialogLayout dialogLayout;
  public Label notificationText;
  public TitledPane exceptionArea;
  public TextArea exceptionTextArea;
  public VBox immediateNotificationRoot;
  private Runnable closeListener;

  public ImmediateNotificationController() {
    dialogLayout = new DialogLayout();
  }

  public void initialize() {
    exceptionArea.managedProperty().bind(exceptionArea.visibleProperty());
    exceptionTextArea.managedProperty().bind(exceptionArea.visibleProperty());
    notificationText.managedProperty().bind(notificationText.visibleProperty());
    exceptionTextArea.maxWidthProperty().bind(exceptionArea.prefWidthProperty());
    notificationText.maxWidthProperty().bind(dialogLayout.prefWidthProperty());
    exceptionArea.maxWidthProperty().bind(dialogLayout.prefWidthProperty());

    dialogLayout.setBody(immediateNotificationRoot);
  }

  public ImmediateNotificationController setNotification(ImmediateNotification notification) {
    StringWriter writer = new StringWriter();
    Throwable throwable = notification.getThrowable();
    if (throwable != null) {
      throwable.printStackTrace(new PrintWriter(writer));
      exceptionTextArea.setVisible(true);
      exceptionTextArea.setText(writer.toString());
      exceptionArea.setExpanded(false);
    } else {
      exceptionTextArea.setVisible(false);
      exceptionArea.setVisible(false);
    }

    dialogLayout.setHeading(new Label(notification.getTitle()));
    notificationText.setText(notification.getText());

    Optional.ofNullable(notification.getActions())
        .map(actions -> actions.stream().map(this::createButton).collect(Collectors.toList()))
        .ifPresent(dialogLayout::setActions);
    if (notification.getCustomUI() != null) {
      immediateNotificationRoot.getChildren().add(notification.getCustomUI());
    }
    return this;
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
        button.getStyleClass().add("dialog-accept");
        ButtonBar.setButtonData(button, ButtonBar.ButtonData.OK_DONE);
        break;
    }

    // Until implemented
    if (action instanceof ReportAction) {
      button.setDisable(true);
    }

    return button;
  }

  private void dismiss() {
    closeListener.run();
  }

  public Region getRoot() {
    return immediateNotificationRoot;
  }

  public ImmediateNotificationController setCloseListener(Runnable closeListener) {
    this.closeListener = closeListener;
    return this;
  }

  public DialogLayout getDialogLayout() {
    return dialogLayout;
  }
}
