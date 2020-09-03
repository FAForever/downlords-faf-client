package com.faforever.client.login;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Irc;
import com.faforever.client.config.ClientProperties.Replay;
import com.faforever.client.config.ClientProperties.Server;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.update.ClientConfiguration;
import com.faforever.client.update.ClientConfiguration.Endpoints;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.update.DownloadUpdateTask;
import com.faforever.client.update.UpdateInfo;
import com.faforever.client.update.Version;
import com.faforever.client.user.UserService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class LoginController implements Controller<Pane> {

  private static final Pattern EMAIL_REGEX = Pattern.compile(".*[@].*[.].*");
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final UserService userService;
  private final PreferencesService preferencesService;
  private final PlatformService platformService;
  private final ClientProperties clientProperties;
  private final I18n i18n;
  private final ClientUpdateService clientUpdateService;
  private CompletableFuture<Void> initializeFuture;
  private Boolean loginAllowed;

  public Pane loginFormPane;
  public Pane loginProgressPane;
  public CheckBox autoLoginCheckBox;
  public TextField usernameInput;
  public TextField passwordInput;
  public ComboBox<ClientConfiguration.Endpoints> environmentComboBox;
  public Button loginButton;
  public Button downloadUpdateButton;
  public Label loginErrorLabel;
  public Pane loginRoot;
  public GridPane serverConfigPane;
  public TextField serverHostField;
  public TextField serverPortField;
  public TextField replayServerHostField;
  public TextField replayServerPortField;
  public TextField ircServerHostField;
  public TextField ircServerPortField;
  public TextField apiBaseUrlField;
  public Button serverStatusButton;

  @VisibleForTesting
  CompletableFuture<UpdateInfo> updateInfoFuture;

  public LoginController(
      UserService userService,
      PreferencesService preferencesService,
      PlatformService platformService,
      ClientProperties clientProperties,
      I18n i18n, ClientUpdateService clientUpdateService) {
    this.userService = userService;
    this.preferencesService = preferencesService;
    this.platformService = platformService;
    this.clientProperties = clientProperties;
    this.i18n = i18n;
    this.clientUpdateService = clientUpdateService;
  }

  public void initialize() {
    updateInfoFuture = clientUpdateService.getNewestUpdate();

    downloadUpdateButton.managedProperty().bind(downloadUpdateButton.visibleProperty());
    downloadUpdateButton.setVisible(false);

    loginErrorLabel.managedProperty().bind(loginErrorLabel.visibleProperty());
    loginErrorLabel.setVisible(false);

    loginFormPane.managedProperty().bind(loginFormPane.visibleProperty());

    loginProgressPane.managedProperty().bind(loginProgressPane.visibleProperty());
    loginProgressPane.setVisible(false);

    serverConfigPane.managedProperty().bind(serverConfigPane.visibleProperty());
    serverConfigPane.setVisible(false);

    serverStatusButton.managedProperty().bind(serverStatusButton.visibleProperty());
    serverStatusButton.setVisible(clientProperties.getStatusPageUrl() != null);

    // fallback values if configuration is not read from remote
    populateEndpointFields(
        clientProperties.getServer().getHost(),
        clientProperties.getServer().getPort(),
        clientProperties.getReplay().getRemoteHost(),
        clientProperties.getReplay().getRemotePort(),
        clientProperties.getIrc().getHost(),
        clientProperties.getIrc().getPort(),
        clientProperties.getApi().getBaseUrl()
    );

    environmentComboBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(Endpoints endpoints) {
        return endpoints == null ? null : endpoints.getName();
      }

      @Override
      public Endpoints fromString(String string) {
        throw new UnsupportedOperationException("Not supported");
      }
    });

    ReadOnlyObjectProperty<Endpoints> selectedEndpointProperty = environmentComboBox.getSelectionModel().selectedItemProperty();

    selectedEndpointProperty.addListener(observable -> {
      Endpoints endpoints = environmentComboBox.getSelectionModel().getSelectedItem();

      if (endpoints == null) {
        return;
      }

      serverHostField.setText(endpoints.getLobby().getHost());
      serverPortField.setText(String.valueOf(endpoints.getLobby().getPort()));

      replayServerHostField.setText(endpoints.getLiveReplay().getHost());
      replayServerPortField.setText(String.valueOf(endpoints.getLiveReplay().getPort()));

      ircServerHostField.setText(endpoints.getIrc().getHost());
      ircServerPortField.setText(String.valueOf(endpoints.getIrc().getPort()));

      apiBaseUrlField.setText(endpoints.getApi().getUrl());
    });


    if (clientProperties.isUseRemotePreferences()) {
      initializeFuture = preferencesService.getRemotePreferencesAsync()
          .thenApply(clientConfiguration -> {
            String minimumVersion = clientConfiguration.getLatestRelease().getMinimumVersion();
            boolean shouldUpdate = false;
            try {
              shouldUpdate = Version.shouldUpdate(Version.getCurrentVersion(), minimumVersion);
            } catch (Exception e) {
              log.error("Something went wrong checking for update", e);
            }
            if (minimumVersion != null && shouldUpdate) {
              loginAllowed = false;
              Platform.runLater(() -> showClientOutdatedPane(minimumVersion));
            } else {
              loginAllowed = true;
            }
            return clientConfiguration;
          })
          .thenAccept(clientConfiguration -> Platform.runLater(() -> {
            Endpoints defaultEndpoint = clientConfiguration.getEndpoints().get(0);
            environmentComboBox.getItems().addAll(clientConfiguration.getEndpoints());
            environmentComboBox.getSelectionModel().select(defaultEndpoint);
          })).exceptionally(throwable -> {
            log.warn("Could not read remote preferences", throwable);
            loginAllowed = true;
            return null;
          });
    } else {
      loginAllowed = true;
      initializeFuture = CompletableFuture.completedFuture(null);
    }
  }

  private void showClientOutdatedPane(String minimumVersion) {
    Platform.runLater(() -> {
      loginErrorLabel.setText(i18n.get("login.clientTooOldError", Version.getCurrentVersion(), minimumVersion));
      loginErrorLabel.setVisible(true);
      downloadUpdateButton.setVisible(true);
      loginFormPane.setDisable(true);
      log.warn("Update required");
    });
  }

  private void populateEndpointFields(
      String serverHost,
      int serverPort,
      String replayServerHost,
      int replayServerPort,
      String ircServerHost,
      int ircServerPort,
      String apiBaseUrl
  ) {
    JavaFxUtil.runLater(() -> {
      serverHostField.setText(serverHost);
      serverPortField.setText(String.valueOf(serverPort));
      replayServerHostField.setText(replayServerHost);
      replayServerPortField.setText(String.valueOf(replayServerPort));
      ircServerHostField.setText(ircServerHost);
      ircServerPortField.setText(String.valueOf(ircServerPort));
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

    initializeFuture.thenRun(() -> {
      if (loginAllowed == null) {
        log.error("loginAllowed not set for unknown reason. Possible race condition detected. Enabling login now to preserve user experience.");
        loginAllowed = true;
      }

      if (loginAllowed && loginPrefs.getAutoLogin() && !isNullOrEmpty(username) && !isNullOrEmpty(password)) {
        login(username, password, true);
      } else if (isNullOrEmpty(username)) {
        usernameInput.requestFocus();
      } else {
        passwordInput.requestFocus();
      }
    });
  }

  private void setShowLoginProgress(boolean show) {
    if (show) {
      loginErrorLabel.setVisible(false);
    }
    loginFormPane.setVisible(!show);
    loginProgressPane.setVisible(show);
    loginButton.setDisable(show);
  }

  private void login(String username, String password, boolean autoLogin) {
    setShowLoginProgress(true);
    if (EMAIL_REGEX.matcher(username).matches()) {
      onLoginWithEmail();
      return;
    }
    userService.login(username, password, autoLogin)
        .exceptionally(throwable -> {
          onLoginFailed(throwable);
          return null;
        });
  }

  private void onLoginWithEmail() {
    loginErrorLabel.setText(i18n.get("login.withEmailWarning"));
    loginErrorLabel.setVisible(true);
    setShowLoginProgress(false);
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

    Irc irc = clientProperties.getIrc();
    irc.setHost(ircServerHostField.getText());
    irc.setPort(Integer.parseInt(ircServerPortField.getText()));

    clientProperties.getApi().setBaseUrl(apiBaseUrlField.getText());

    login(username, password, autoLogin);
  }

  public void onCancelLoginButtonClicked() {
    userService.cancelLogin();
    setShowLoginProgress(false);
  }

  public void onDownloadUpdateButtonClicked() {
    downloadUpdateButton.setOnAction(event -> {
    });
    log.info("Downloading update");
    updateInfoFuture
        .thenAccept(updateInfo -> {
          DownloadUpdateTask downloadUpdateTask = clientUpdateService.downloadAndInstallInBackground(updateInfo);

          if (downloadUpdateTask != null) {
            downloadUpdateButton.textProperty().bind(
                Bindings.createStringBinding(() -> downloadUpdateTask.getProgress() == -1 ?
                        i18n.get("login.button.downloadPreparing") :
                        i18n.get("login.button.downloadProgress", downloadUpdateTask.getProgress()),
                    downloadUpdateTask.progressProperty()));
          }
        });
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

  public void seeServerStatus() {
    String statusPageUrl = clientProperties.getStatusPageUrl();
    if (statusPageUrl == null) {
      return;
    }
    platformService.showDocument(statusPageUrl);
  }
}
