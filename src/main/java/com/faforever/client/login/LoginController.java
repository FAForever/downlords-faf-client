package com.faforever.client.login;

import com.faforever.client.fx.SceneFactory;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.MainController;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.JavaFxUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import static com.faforever.client.fx.SceneFactoryImpl.WindowButtonType.CLOSE;
import static com.faforever.client.fx.SceneFactoryImpl.WindowButtonType.MINIMIZE;

public class LoginController {

  @FXML
  Pane loginFormPane;

  @FXML
  Pane loginProgressPane;

  @FXML
  CheckBox autoLoginCheckBox;

  @FXML
  TextField usernameInput;

  @FXML
  TextField passwordInput;

  @FXML
  Button loginButton;

  @FXML
  Parent loginRoot;

  @Autowired
  MainController mainController;

  @Autowired
  I18n i18n;

  @Autowired
  Environment environment;

  @Autowired
  UserService userService;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  SceneFactory sceneFactory;

  private Stage stage;

  /**
   * If used typed something into the password field, this will be set to true.
   */
  private boolean isPlainTextPassword;
  private double xOffset;
  private double yOffset;

  @FXML
  private void initialize() {
    passwordInput.setOnKeyPressed(event -> isPlainTextPassword = true);
    loginProgressPane.setVisible(false);
  }

  public void display(Stage stage) {
    this.stage = stage;

    Scene scene = sceneFactory.createScene(stage, loginRoot, false, MINIMIZE, CLOSE);

    stage.setScene(scene);
    stage.setTitle(i18n.get("login.title"));
    stage.setResizable(false);

    fillForm();
    if (autoLoginCheckBox.isSelected()) {
      login(usernameInput.getText(), passwordInput.getText(), true);
    }

    configureWindowDragable(stage);
    stage.show();
    JavaFxUtil.centerOnScreen(stage);

    usernameInput.requestFocus();
  }

  private void configureWindowDragable(final Stage stage) {
    loginRoot.setOnMousePressed(event -> {
      xOffset = stage.getX() - event.getScreenX();
      yOffset = stage.getY() - event.getScreenY();
    });
    loginRoot.setOnMouseDragged(event -> {
      stage.setX(event.getScreenX() + xOffset);
      stage.setY(event.getScreenY() + yOffset);
    });
  }

  private void fillForm() {
    LoginPrefs loginPrefs = preferencesService.getPreferences().getLoginPrefs();
    String username = loginPrefs.getUsername();
    String password = loginPrefs.getPassword();

    boolean autoLogin = StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password);
    autoLoginCheckBox.setSelected(autoLogin);

    usernameInput.setText(StringUtils.defaultString(username));
    passwordInput.setText(StringUtils.defaultString(password));
  }

  @FXML
  private void loginButtonClicked(ActionEvent actionEvent) {
    String username = usernameInput.getText();
    String password = passwordInput.getText();

    if (isPlainTextPassword) {
      password = DigestUtils.sha256Hex(password);
    }

    boolean autoLogin = autoLoginCheckBox.isSelected();

    login(username, password, autoLogin);
  }

  private void login(String username, String password, boolean autoLogin) {
    onLoginProgress();

    userService.login(username, password, autoLogin, new Callback<Void>() {
      @Override
      public void success(Void result) {
        onLoginSucceeded();
      }

      @Override
      public void error(Throwable e) {
        onLoginFailed(e);
      }
    });
  }

  private void onLoginSucceeded() {
    mainController.display(stage);
  }

  private void onLoginFailed(Throwable e) {
    Dialog<Void> loginFailedDialog = new Dialog<>();
    loginFailedDialog.setTitle(i18n.get("login.failed.title"));
    loginFailedDialog.setContentText(i18n.get("login.failed.message"));
    loginFailedDialog.show();

    loginFormPane.setVisible(true);
    loginProgressPane.setVisible(false);
    loginButton.setDisable(false);
  }

  private void onLoginProgress() {
    loginFormPane.setVisible(false);
    loginProgressPane.setVisible(true);
    loginButton.setDisable(true);
  }

  @FXML
  void cancelButtonClicked(ActionEvent actionEvent) {
    // TODO cancel login
  }

  @FXML
  void onCloseButtonClicked(ActionEvent actionEvent) {
    stage.close();
  }

  @FXML
  void onMinimizeButtonClicked(ActionEvent actionEvent) {
    stage.setIconified(true);
  }
}
