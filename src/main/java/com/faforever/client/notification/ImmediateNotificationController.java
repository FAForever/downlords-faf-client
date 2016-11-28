package com.faforever.client.notification;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.WebViewConfigurer;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.PrintWriter;
import java.io.StringWriter;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ImmediateNotificationController implements Controller<Node> {

  public WebView errorMessageView;
  public Node exceptionPane;
  public TextArea exceptionTextArea;
  public Label titleLabel;
  public ButtonBar buttonBar;
  public Region notificationRoot;

  private final WebViewConfigurer webViewConfigurer;

  @Inject
  public ImmediateNotificationController(WebViewConfigurer webViewConfigurer) {
    this.webViewConfigurer = webViewConfigurer;
  }

  public void initialize() {
    exceptionPane.managedProperty().bind(exceptionPane.visibleProperty());
    webViewConfigurer.configureWebView(errorMessageView);
  }

  public void setNotification(ImmediateNotification notification) {
    StringWriter writer = new StringWriter();
    Throwable throwable = notification.getThrowable();
    if (throwable != null) {
      throwable.printStackTrace(new PrintWriter(writer));
      exceptionPane.setVisible(true);
      exceptionTextArea.setText(writer.toString());
    } else {
      exceptionPane.setVisible(false);
    }

    titleLabel.setText(notification.getTitle());
    Platform.runLater(() -> errorMessageView.getEngine().loadContent(notification.getText()));

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

    // Until implemented
    if (action instanceof ReportAction) {
      button.setDisable(true);
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
