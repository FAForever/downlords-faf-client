package com.faforever.client.game;

import com.faforever.client.fx.WindowController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;

import static com.faforever.client.fx.WindowController.WindowButtonType.CLOSE;

public class EnterPasswordController {

  @Resource
  ApplicationContext applicationContext;
  @FXML
  Label loginErrorLabel;
  @FXML
  Label titleLabel;
  @FXML
  TextField passwordField;
  @FXML
  ButtonBar buttonBar;
  @FXML
  Region enterPasswordRoot;
  @FXML
  Button joinButton;
  @FXML
  Button cancelButton;
  private OnPasswordEnteredListener listener;
  private Game game;
  private boolean ignoreRating;

  @FXML
  void initialize() {
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

  @FXML
  void onJoinButtonClicked() {
    if (listener == null) {
      throw new IllegalStateException("No listener has been set");
    }
    listener.onPasswordEntered(game, passwordField.getText(), ignoreRating);
    getRoot().getScene().getWindow().hide();
  }

  public Region getRoot() {
    return enterPasswordRoot;
  }

  @FXML
  void onCancelButtonClicked() {
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

    WindowController windowController = applicationContext.getBean(WindowController.class);
    windowController.configure(userInfoWindow, getRoot(), true, CLOSE);

    userInfoWindow.show();
  }

  interface OnPasswordEnteredListener {

    void onPasswordEntered(Game game, String password, boolean ignoreRating);
  }
}
