package com.faforever.client.login;

import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.user.UserService;
import com.faforever.client.util.JavaFxUtil;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;

public class LoginController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
  Label loginErrorLabel;
  @FXML
  Pane loginRoot;

  @Resource
  UserService userService;
  @Resource
  PreferencesService preferencesService;

  private boolean autoLogin;

  @FXML
  private void initialize() {
    loginProgressPane.setVisible(false);
    loginErrorLabel.managedProperty().bind(loginErrorLabel.visibleProperty());
    autoLogin = true;
  }

  public void display() {
    setShowLoginProgress(false);

    LoginPrefs loginPrefs = preferencesService.getPreferences().getLogin();
    String username = loginPrefs.getUsername();
    String password = loginPrefs.getPassword();
    boolean isAutoLogin = loginPrefs.getAutoLogin();

    // Fill the form even if autoLogin is true, since user may cancel the login
    usernameInput.setText(Strings.nullToEmpty(username));
    autoLoginCheckBox.setSelected(isAutoLogin);

    if (loginPrefs.getAutoLogin() && !isNullOrEmpty(username) && !isNullOrEmpty(password) && autoLogin) {
      autoLogin = false;
      login(username, password, true);
    } else if (isNullOrEmpty(username)) {
      usernameInput.requestFocus();
    } else {
      passwordInput.requestFocus();
    }
  }

  private void setShowLoginProgress(boolean show) {
    loginFormPane.setVisible(!show);
    loginProgressPane.setVisible(show);
    loginButton.setDisable(show);
    loginErrorLabel.setVisible(false);
  }

  private void login(String username, String password, boolean autoLogin) {
    setShowLoginProgress(true);

    userService.login(username, password, autoLogin)
        .exceptionally(throwable -> {
          onLoginFailed(throwable);
          return null;
        });
  }

  private void onLoginFailed(Throwable e) {
    logger.warn("Login failed", e);
    Platform.runLater(() -> {
      loginErrorLabel.setText(e.getCause().getLocalizedMessage());

      setShowLoginProgress(false);
      loginErrorLabel.setVisible(true);
    });
  }

  @FXML
  void loginButtonClicked() {
    String username = usernameInput.getText();
    String password = passwordInput.getText();

    password = Hashing.sha256().hashString(password, UTF_8).toString();

    boolean autoLogin = autoLoginCheckBox.isSelected();

    login(username, password, autoLogin);
  }

  @FXML
  public void onCancelLoginButtonClicked() {
    userService.cancelLogin();
    setShowLoginProgress(false);
  }

  public Pane getRoot() {
    return loginRoot;
  }
}
