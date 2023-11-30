package com.faforever.client.notification;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.notification.Action.Type;
import com.faforever.client.ui.dialog.DialogLayout;
import com.faforever.client.update.Version;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ImmediateNotificationController extends NodeController<Node> {

  private final DialogLayout dialogLayout = new DialogLayout();
  public Label notificationText;
  public Label versionText;
  public TitledPane exceptionArea;
  public TextArea exceptionTextArea;
  public Label helpText;
  public VBox immediateNotificationRoot;
  private Runnable closeListener;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(exceptionArea, notificationText, helpText, versionText);

    dialogLayout.setMaxWidth(650);

    dialogLayout.setBody(immediateNotificationRoot);
  }

  public ImmediateNotificationController setNotification(ImmediateNotification notification) {
    StringWriter writer = new StringWriter();
    Throwable throwable = notification.getThrowable();
    if (throwable != null) {
      throwable.printStackTrace(new PrintWriter(writer));
      exceptionTextArea.setVisible(true);
      exceptionTextArea.setText(String.format("Client Version: %s%n%s", Version.getCurrentVersion(), writer));
      versionText.setText(String.format("v%s", Version.getCurrentVersion()));
    } else {
      exceptionTextArea.setVisible(false);
      exceptionArea.setVisible(false);
      helpText.setVisible(false);
      versionText.setVisible(false);
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

    if (action.getType() == Type.OK_DONE) {
      button.getStyleClass().add("dialog-accept");
      ButtonBar.setButtonData(button, ButtonData.OK_DONE);
    }

    return button;
  }

  private void dismiss() {
    closeListener.run();
  }

  @Override
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
