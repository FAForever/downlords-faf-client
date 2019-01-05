package com.faforever.client.login;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Replay;
import com.faforever.client.config.ClientProperties.Server;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.update.ClientConfiguration.Endpoints;
import com.faforever.client.user.UserService;
import com.google.common.base.Strings;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CancellationException;

import static com.google.common.base.Strings.isNullOrEmpty;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class LoginController implements Controller<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final UserService userService;
  private final PreferencesService preferencesService;
  private final PlatformService platformService;
  private final ClientProperties clientProperties;

  public Pane loginFormPane;
  public Pane loginProgressPane;
  public CheckBox autoLoginCheckBox;
  public TextField usernameInput;
  public TextField passwordInput;
  public Button loginButton;
  public Label loginErrorLabel;
  public Pane loginRoot;
  public GridPane serverConfigPane;
  public TextField serverHostField;
  public TextField serverPortField;
  public TextField replayServerHostField;
  public TextField replayServerPortField;
  public TextField apiBaseUrlField;

  public LoginController(
      UserService userService,
      PreferencesService preferencesService,
      PlatformService platformService,
      ClientProperties clientProperties
  ) {
    this.userService = userService;
    this.preferencesService = preferencesService;
    this.platformService = platformService;
    this.clientProperties = clientProperties;
  }

  public void initialize() {
    loginErrorLabel.managedProperty().bind(loginErrorLabel.visibleProperty());
    loginErrorLabel.setVisible(false);

    loginFormPane.managedProperty().bind(loginFormPane.visibleProperty());

    loginProgressPane.managedProperty().bind(loginProgressPane.visibleProperty());
    loginProgressPane.setVisible(false);

    serverConfigPane.managedProperty().bind(serverConfigPane.visibleProperty());
    serverConfigPane.setVisible(false);

    populateEndpointFields(
        clientProperties.getServer().getHost(),
        clientProperties.getServer().getPort(),
        clientProperties.getReplay().getRemoteHost(),
        clientProperties.getReplay().getRemotePort(),
        clientProperties.getApi().getBaseUrl()
    );

    if (clientProperties.isUseRemotePreferences()) {
      preferencesService.getRemotePreferences().thenAccept(clientConfiguration -> {
        Endpoints defaultEndpoint = clientConfiguration.getEndpoints().get(0);
        populateEndpointFields(
            defaultEndpoint.getLobby().getHost(),
            clientConfiguration.getEndpoints().get(0).getLobby().getPort(),
            clientConfiguration.getEndpoints().get(0).getLiveReplay().getHost(),
            clientConfiguration.getEndpoints().get(0).getLiveReplay().getPort(),
            clientConfiguration.getEndpoints().get(0).getApi().getUrl()
        );
      }).exceptionally(throwable -> {
        log.warn("Could not read remote preferences");
        return null;
      });
    }
  }

  private void populateEndpointFields(
      String serverHost,
      int serverPort,
      String replayServerHost,
      int replayServerPort,
      String apiBaseUrl
  ) {
    JavaFxUtil.runLater(() -> {
      serverHostField.setText(serverHost);
      serverPortField.setText(String.valueOf(serverPort));
      replayServerHostField.setText(replayServerHost);
      replayServerPortField.setText(String.valueOf(replayServerPort));
      apiBaseUrlField.setText(apiBaseUrl);
    });
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
      if (e instanceof CancellationException) {
        loginErrorLabel.setVisible(false);
      } else {
        if (e instanceof LoginFailedException) {
          loginErrorLabel.setText(e.getMessage());
        } else {
          loginErrorLabel.setText(e.getCause().getLocalizedMessage());
        }
        loginErrorLabel.setVisible(true);
      }

      setShowLoginProgress(false);
    });
  }

  public void onLoginButtonClicked() {
    String username = usernameInput.getText();
    String password = passwordInput.getText();

    boolean autoLogin = autoLoginCheckBox.isSelected();

    Server server = clientProperties.getServer();
    server.setHost(serverHostField.getText());
    server.setPort(Integer.parseInt(serverPortField.getText()));

    Replay replay = clientProperties.getReplay();
    replay.setRemoteHost(replayServerHostField.getText());
    replay.setRemotePort(Integer.parseInt(replayServerPortField.getText()));

    clientProperties.getApi().setBaseUrl(apiBaseUrlField.getText());

    login(username, password, autoLogin);
  }

  public void onCancelLoginButtonClicked() {
    userService.cancelLogin();
    setShowLoginProgress(false);
  }

  public Pane getRoot() {
    return loginRoot;
  }

  public void forgotLoginClicked() {
    platformService.showDocument(clientProperties.getWebsite().getForgotPasswordUrl());
  }

  public void createNewAccountClicked() {
    platformService.showDocument(clientProperties.getWebsite().getCreateAccountUrl());
  }

  public void onMouseClicked(MouseEvent event) {
    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
      serverConfigPane.setVisible(true);
    }
  }
}
