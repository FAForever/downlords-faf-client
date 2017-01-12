package com.faforever.client.login;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.UserService;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CancellationException;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LoginController implements Controller<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final UserService userService;
  private final PreferencesService preferencesService;
  public Pane loginFormPane;
  public Pane loginProgressPane;
  public CheckBox autoLoginCheckBox;
  public TextField usernameInput;
  public Button forgotLogin;
  public TextField passwordInput;
  public Button loginButton;
  public Button createAccountButton;
  public Label loginErrorLabel;
  public Pane loginRoot;
  private PlatformService platformService;
  private String createUrl;
  private String forgotLoginUrl;

  @Inject
  public LoginController(UserService userService, PreferencesService preferencesService, PlatformService platformService, @Value("${login.createAccountUrl}") String createUrl, @Value("${login.forgotLoginUrl}") String forgotLoginUrl) {
    this.userService = userService;
    this.preferencesService = preferencesService;
    this.platformService = platformService;
    this.createUrl = createUrl;
    this.forgotLoginUrl = forgotLoginUrl;
  }

  public void initialize() {
    loginProgressPane.setVisible(false);
    loginErrorLabel.managedProperty().bind(loginErrorLabel.visibleProperty());
    loginErrorLabel.setVisible(false);
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

    if (loginPrefs.getAutoLogin() && !isNullOrEmpty(username) && !isNullOrEmpty(password)) {
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
      if (!(e instanceof CancellationException)) {
        loginErrorLabel.setText(e.getCause().getLocalizedMessage());
        loginErrorLabel.setVisible(true);
        //TODO: fix wrapping issue in loginErrorLable
      } else {
        loginErrorLabel.setVisible(false);
      }

      setShowLoginProgress(false);
    });
  }

  public void loginButtonClicked() {
    String username = usernameInput.getText();
    String password = passwordInput.getText();

    password = Hashing.sha256().hashString(password, UTF_8).toString();

    boolean autoLogin = autoLoginCheckBox.isSelected();

    login(username, password, autoLogin);
  }

  public void onCancelLoginButtonClicked() {
    userService.cancelLogin();
    setShowLoginProgress(false);
  }

  public Pane getRoot() {
    return loginRoot;
  }

  public void createAccountButtonClicked(ActionEvent actionEvent) {
    platformService.showDocument(createUrl);
  }

  public void forgotLoginButtonClicked(ActionEvent actionEvent) {
    platformService.showDocument(forgotLoginUrl);
  }
}
