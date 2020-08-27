package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class EnterPasswordController implements Controller<Node> {

  private final UiService uiService;
  public Label loginErrorLabel;
  public Label titleLabel;
  public TextField passwordField;
  public ButtonBar buttonBar;
  public Parent enterPasswordRoot;
  public Button joinButton;
  public Button cancelButton;
  private OnPasswordEnteredListener listener;
  private Game game;
  private boolean ignoreRating;

  public void initialize() {
    loginErrorLabel.setVisible(false); // ToDo: manage negative logins
    loginErrorLabel.managedProperty().bind(loginErrorLabel.visibleProperty());
    joinButton.disableProperty().bind(passwordField.textProperty().isEmpty());
  }

  void setOnPasswordEnteredListener(OnPasswordEnteredListener listener) {
    if (this.listener != null) {
      throw new IllegalStateException("Listener has already been set");
    }
    this.listener = listener;
  }

  public void onJoinButtonClicked() {
    if (listener == null) {
      throw new IllegalStateException("No listener has been set");
    }
    listener.onPasswordEntered(game, passwordField.getText(), ignoreRating);
    getRoot().getScene().getWindow().hide();
  }

  public Parent getRoot() {
    return enterPasswordRoot;
  }

  public void onCancelButtonClicked() {
    getRoot().getScene().getWindow().hide();
  }

  public void setGame(Game game) {
    this.game = game;
  }

  public void setIgnoreRating(boolean ignoreRating) {
    this.ignoreRating = ignoreRating;
  }

  public void showPasswordDialog(Window owner) {
    Stage userInfoWindow = new Stage(StageStyle.TRANSPARENT);
    userInfoWindow.initModality(Modality.NONE);
    userInfoWindow.initOwner(owner);

    Scene scene = uiService.createScene(getRoot());
    userInfoWindow.setScene(scene);
    userInfoWindow.show();
  }

  interface OnPasswordEnteredListener {

    void onPasswordEntered(Game game, String password, boolean ignoreRating);
  }
}
