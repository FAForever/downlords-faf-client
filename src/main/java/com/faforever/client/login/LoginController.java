package com.faforever.client.login;

import com.faforever.client.fx.SceneFactory;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.JavaFxUtil;
import com.google.common.base.Strings;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;

import static com.faforever.client.fx.WindowDecorator.WindowButtonType.CLOSE;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.MINIMIZE;
import static com.google.common.base.Strings.isNullOrEmpty;

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
  Region loginRoot;

  @Resource
  I18n i18n;
  @Resource
  UserService userService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  SceneFactory sceneFactory;
  @Resource
  Stage stage;
  private boolean loggedOut;

  @FXML
  private void initialize() {
    loginProgressPane.setVisible(false);
    loginErrorLabel.managedProperty().bind(loginErrorLabel.visibleProperty());
  }

  @PostConstruct
  void postConstruct() {
    userService.addOnLogoutListener(this::onLoggedOut);
  }

  private void onLoggedOut() {
    loggedOut = true;
    display();
  }

  public void display() {
    sceneFactory.createScene(stage, loginRoot, false, MINIMIZE, CLOSE);
    stage.sizeToScene();

    stage.setTitle(i18n.get("login.title"));
    stage.setResizable(false);
    setShowLoginProgress(false);

    LoginPrefs loginPrefs = preferencesService.getPreferences().getLogin();
    String username = loginPrefs.getUsername();
    String password = loginPrefs.getPassword();
    boolean isAutoLogin = loginPrefs.getAutoLogin();

    // Fill the form even if autoLogin is true, since user may cancel the login
    usernameInput.setText(Strings.nullToEmpty(username));
    autoLoginCheckBox.setSelected(isAutoLogin);

    stage.show();
    JavaFxUtil.centerOnScreen(stage);

    if (loginPrefs.getAutoLogin() && !isNullOrEmpty(username) && !isNullOrEmpty(password) && !loggedOut) {
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

    password = DigestUtils.sha256Hex(password);

    boolean autoLogin = autoLoginCheckBox.isSelected();

    login(username, password, autoLogin);
  }

  @FXML
  void onCloseButtonClicked() {
    stage.close();
  }

  @FXML
  void onMinimizeButtonClicked() {
    stage.setIconified(true);
  }

  @FXML
  public void onCancelLoginButtonClicked() {
    userService.cancelLogin();
    setShowLoginProgress(false);
  }
}
