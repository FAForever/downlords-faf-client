package com.faforever.client.notification;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.ui.dialog.DialogLayout;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
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
public class ServerNotificationController extends NodeController<Node> {

  private final WebViewConfigurer webViewConfigurer;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final DialogLayout dialogLayout = new DialogLayout();
  public WebView errorMessageView;
  public Label exceptionAreaTitleLabel;
  public TextArea exceptionTextArea;
  public VBox serverNotificationRoot;
  private Runnable closeListener;

  @Override
  protected void onInitialize() {
    exceptionAreaTitleLabel.managedProperty().bind(exceptionAreaTitleLabel.visibleProperty());
    exceptionAreaTitleLabel.visibleProperty().bind(exceptionTextArea.visibleProperty());
    exceptionTextArea.managedProperty().bind(exceptionTextArea.visibleProperty());
    webViewConfigurer.configureWebView(errorMessageView);
    errorMessageView.managedProperty().bind(errorMessageView.visibleProperty());

    dialogLayout.setBody(serverNotificationRoot);
  }

  public ServerNotificationController setNotification(ImmediateNotification notification) {
    StringWriter writer = new StringWriter();
    Throwable throwable = notification.getThrowable();
    if (throwable != null) {
      throwable.printStackTrace(new PrintWriter(writer));
      exceptionTextArea.setVisible(true);
      exceptionTextArea.setText(writer.toString());
    } else {
      exceptionTextArea.setVisible(false);
    }

    dialogLayout.setHeading(new Label(notification.getTitle()));
    fxApplicationThreadExecutor.execute(() -> errorMessageView.getEngine().loadContent(notification.getText()));

    Optional.ofNullable(notification.getActions())
        .map(actions -> actions.stream().map(this::createButton).collect(Collectors.toList()))
        .ifPresent(dialogLayout::setActions);
    if (notification.getCustomUI() != null) {
      serverNotificationRoot.getChildren().add(notification.getCustomUI());
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

    return button;
  }

  private void dismiss() {
    closeListener.run();
  }

  @Override
  public Region getRoot() {
    return serverNotificationRoot;
  }

  public ServerNotificationController setCloseListener(Runnable closeListener) {
    this.closeListener = closeListener;
    return this;
  }

  public DialogLayout getDialogLayout() {
    return dialogLayout;
  }
}
