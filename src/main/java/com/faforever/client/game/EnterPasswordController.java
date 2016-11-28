package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.WindowController;
import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static com.faforever.client.fx.WindowController.WindowButtonType.CLOSE;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EnterPasswordController implements Controller<Node> {

  public Label loginErrorLabel;
  public Label titleLabel;
  public TextField passwordField;
  public ButtonBar buttonBar;
  public Region enterPasswordRoot;
  public Button joinButton;
  public Button cancelButton;

  @Inject
  UiService uiService;
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

  public Region getRoot() {
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

    WindowController windowController = uiService.loadFxml("theme/window.fxml");
    windowController.configure(userInfoWindow, getRoot(), true, CLOSE);

    userInfoWindow.show();
  }

  interface OnPasswordEnteredListener {

    void onPasswordEntered(Game game, String password, boolean ignoreRating);
  }
}
