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

import static com.faforever.client.fx.WindowDecorator.WindowButtonType.CLOSE;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.MINIMIZE;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

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

  @FXML
  private void initialize() {
    loginProgressPane.setVisible(false);
  }

  public void display(Stage stage) {
    this.stage = stage;

    sceneFactory.createScene(stage, loginRoot, false, MINIMIZE, CLOSE);

    stage.setTitle(i18n.get("login.title"));
    stage.setResizable(false);

    LoginPrefs loginPrefs = preferencesService.getPreferences().getLogin();
    String username = loginPrefs.getUsername();
    String password = loginPrefs.getPassword();
    boolean isAutoLogin = loginPrefs.isAutoLogin();

    // Fill the form even if autoLogin is true, since user may cancel the login
    usernameInput.setText(StringUtils.defaultString(username));
    autoLoginCheckBox.setSelected(isAutoLogin);

    if (loginPrefs.isAutoLogin() && isNotEmpty(username) && isNotEmpty(password)) {
      login(username, password, true);
    } else if (isEmpty(username)) {
      usernameInput.requestFocus();
    } else {
      passwordInput.requestFocus();
    }

    stage.show();
    JavaFxUtil.centerOnScreen(stage);
  }

  @FXML
  void loginButtonClicked(ActionEvent actionEvent) {
    String username = usernameInput.getText();
    String password = passwordInput.getText();

    password = DigestUtils.sha256Hex(password);

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
    setShowLoginProgress(true);
  }

  private void setShowLoginProgress(boolean b) {
    loginFormPane.setVisible(!b);
    loginProgressPane.setVisible(b);
    loginButton.setDisable(b);
  }

  @FXML
  void onCloseButtonClicked(ActionEvent actionEvent) {
    stage.close();
  }

  @FXML
  void onMinimizeButtonClicked(ActionEvent actionEvent) {
    stage.setIconified(true);
  }

  @FXML
  public void onCancelLoginButtonClicked(ActionEvent actionEvent) {
    userService.cancelLogin();
    setShowLoginProgress(false);
  }
}
