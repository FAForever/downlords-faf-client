package com.faforever.client.login;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Irc;
import com.faforever.client.config.ClientProperties.Replay;
import com.faforever.client.config.ClientProperties.User;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameRunner;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ServerNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.update.ClientConfiguration.ServerEndpoints;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.update.DownloadUpdateTask;
import com.faforever.client.update.UpdateInfo;
import com.faforever.client.update.Version;
import com.faforever.client.user.LoginService;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.commons.lobby.LoginException;
import com.google.common.annotations.VisibleForTesting;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class LoginController extends NodeController<Pane> {
  private final OperatingSystem operatingSystem;
  private final GameRunner gameRunner;
  private final LoginService loginService;
  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final ClientProperties clientProperties;
  private final I18n i18n;
  private final ClientUpdateService clientUpdateService;
  private final PlatformService platformService;
  private final OAuthValuesReceiver oAuthValuesReceiver;
  private final LoginPrefs loginPrefs;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public Pane messagesContainer;
  public Pane errorPane;
  public Pane loginFormPane;
  public Button loginButton;
  public Pane loginProgressPane;
  public ComboBox<ServerEndpoints> environmentComboBox;
  public Button downloadUpdateButton;
  public Button playOfflineButton;
  public Label loginErrorLabel;
  public Pane loginRoot;
  public GridPane serverConfigPane;
  public TextField userUrlField;
  public TextField replayServerHostField;
  public TextField replayServerPortField;
  public TextField ircServerHostField;
  public TextField ircServerPortField;
  public TextField apiBaseUrlField;
  public TextField oauthBaseUrlField;
  public CheckBox rememberMeCheckBox;
  @VisibleForTesting
  CompletableFuture<UpdateInfo> updateInfoFuture;
  private CompletableFuture<Void> initializeFuture;
  private CompletableFuture<Void> loginFuture;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(downloadUpdateButton, loginErrorLabel, loginFormPane,
        serverConfigPane, errorPane, loginProgressPane, messagesContainer, loginButton);
    updateInfoFuture = clientUpdateService.getNewestUpdate();

    messagesContainer.setVisible(false);
    downloadUpdateButton.setVisible(false);
    errorPane.setVisible(false);
    loginErrorLabel.setVisible(false);
    serverConfigPane.setVisible(false);
    rememberMeCheckBox.setSelected(loginPrefs.isRememberMe());
    loginPrefs.rememberMeProperty().bindBidirectional(rememberMeCheckBox.selectedProperty());

    // fallback values if configuration is not read from remote
    populateEndpointFields();

    environmentComboBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(ServerEndpoints serverEndpoints) {
        return serverEndpoints == null ? null : serverEndpoints.getName();
      }

      @Override
      public ServerEndpoints fromString(String string) {
        throw new UnsupportedOperationException("Not supported");
      }
    });

    ReadOnlyObjectProperty<ServerEndpoints> selectedEndpointProperty = environmentComboBox.getSelectionModel()
        .selectedItemProperty();

    selectedEndpointProperty.addListener(observable -> {
      ServerEndpoints serverEndpoints = environmentComboBox.getSelectionModel().getSelectedItem();

      if (serverEndpoints == null) {
        return;
      }

      clientProperties.updateFromEndpoint(serverEndpoints);

      populateEndpointFields();
    });

    JavaFxUtil.addListener(
        oauthBaseUrlField.textProperty(),
        observable -> clientProperties.getOauth().setBaseUrl(oauthBaseUrlField.getText())
    );

    if (clientProperties.isUseRemotePreferences()) {
      initializeFuture = preferencesService.getRemotePreferencesAsync()
                                           .thenAccept(clientConfiguration -> {
                                             List<ServerEndpoints> endpoints = clientConfiguration.getEndpoints();
                                             ServerEndpoints startingEndpoint = endpoints.stream()
                                                                                         .filter(
                                                                                             endpoint -> endpoint.getName()
                                                                                                                 .equals(
                                                                                                                     loginPrefs.getEndpoint()))
                                                                                         .findFirst()
                                                                                         .orElse(endpoints.getFirst());

                                             clientProperties.updateFromEndpoint(startingEndpoint);

            String minimumVersion = clientConfiguration.getLatestRelease().getMinimumVersion();
            boolean shouldUpdate = false;
            if (minimumVersion != null) {
              shouldUpdate = Version.shouldUpdate(Version.getCurrentVersion(), minimumVersion);

              if (shouldUpdate) {
                fxApplicationThreadExecutor.execute(() -> showClientOutdatedPane(minimumVersion));
                return;
              }
            }

            fxApplicationThreadExecutor.execute(() -> {
              environmentComboBox.getItems().addAll(endpoints);
              environmentComboBox.getSelectionModel().select(startingEndpoint);
              loginPrefs.endpointProperty()
                        .bind(environmentComboBox.getSelectionModel()
                                                 .selectedItemProperty()
                                                 .map(ServerEndpoints::getName)
                                                 .when(showing));
            });

                                             if (loginPrefs.isRememberMe() && loginPrefs.getRefreshToken() != null) {
                                               loginWithToken();
                                             }
                                           }).exceptionally(throwable -> {
            log.error("Could not read remote preferences", throwable);
            return null;
          });
    } else {
      initializeFuture = CompletableFuture.completedFuture(null);
    }
  }

  private void showClientOutdatedPane(String minimumVersion) {
    log.info("Client Update required");
    fxApplicationThreadExecutor.execute(() -> {
      errorPane.setVisible(true);
      loginErrorLabel.setText(i18n.get("login.clientTooOldError", Version.getCurrentVersion(), minimumVersion));
      loginErrorLabel.setVisible(true);

      if (operatingSystem.supportsUpdateInstall()) {
        downloadUpdateButton.setVisible(true);
      }

      loginFormPane.setDisable(true);
      loginFormPane.setVisible(false);
      loginButton.setVisible(false);
      playOfflineButton.setVisible(false);
    });
  }

  private void populateEndpointFields() {
    fxApplicationThreadExecutor.execute(() -> {
      User user = clientProperties.getUser();
      userUrlField.setText(user.getBaseUrl());
      Replay replay = clientProperties.getReplay();
      replayServerHostField.setText(replay.getRemoteHost());
      replayServerPortField.setText(String.valueOf(replay.getRemotePort()));
      Irc irc = clientProperties.getIrc();
      ircServerHostField.setText(irc.getHost());
      ircServerPortField.setText(String.valueOf(irc.getPort()));
      apiBaseUrlField.setText(clientProperties.getApi().getBaseUrl());
      oauthBaseUrlField.setText(clientProperties.getOauth().getBaseUrl());
    });
  }

  public void onDownloadUpdateButtonClicked() {
    downloadUpdateButton.setOnAction(event -> {
    });
    log.info("Downloading update");
    updateInfoFuture
        .thenAccept(updateInfo -> {
          DownloadUpdateTask downloadUpdateTask = clientUpdateService.downloadAndInstallInBackground(updateInfo);

          if (downloadUpdateTask != null) {
            downloadUpdateButton.textProperty()
                .bind(downloadUpdateTask.progressProperty().map(progress -> progress.doubleValue() < 0 ?
                    i18n.get("login.button.downloadPreparing") :
                    i18n.get("login.button.downloadProgress", progress)));
          }
        });
  }

  public void onLoginButtonClicked() {
    if (loginFuture != null && !loginFuture.isDone()) {
      oAuthValuesReceiver.openBrowserToLogin();
      return;
    }

    initializeFuture.join();

    clientProperties.getUser()
        .setBaseUrl(userUrlField.getText());

    clientProperties.getReplay()
        .setRemoteHost(replayServerHostField.getText())
        .setRemotePort(Integer.parseInt(replayServerPortField.getText()));

    clientProperties.getIrc()
        .setHost(ircServerHostField.getText())
        .setPort(Integer.parseInt(ircServerPortField.getText()));

    clientProperties.getApi().setBaseUrl(apiBaseUrlField.getText());
    clientProperties.getOauth().setBaseUrl(oauthBaseUrlField.getText());

    String state = RandomStringUtils.randomAlphanumeric(64, 128);
    String verifier = RandomStringUtils.randomAlphanumeric(64, 128);

    loginFuture = oAuthValuesReceiver.receiveValues(state, verifier).thenCompose(values -> {
      platformService.focusWindow(i18n.get("login.title"));
      String actualState = values.state();
      if (!state.equals(actualState)) {
        throw new IllegalStateException("State returned by the server does not match expected state");
      }
      return loginWithCode(values.code(), values.redirectUri(), verifier).toFuture();
    }).exceptionally(throwable -> onLoginFailed(ConcurrentUtil.unwrapIfCompletionException(throwable)));
  }

  private Mono<Void> loginWithCode(String code, URI redirectUri, String codeVerifier) {
    showLoginProgress();
    return loginService.login(code, codeVerifier, redirectUri);
  }

  private Void onLoginFailed(Throwable throwable) {
    if (loginService.getOwnUser() != null && loginService.getOwnPlayer() != null) {
      log.info("Previous login request failed but user is already logged in", throwable);
      return null;
    }

    if (throwable instanceof SocketTimeoutException) {
      log.info("Login request timed out", throwable);
      notificationService.addImmediateWarnNotification("login.timeout");
    } else if (throwable instanceof LoginException loginException) {
      notificationService.addNotification(
          new ServerNotification(i18n.get("login.failed"), loginException.getMessage(), Severity.ERROR,
                                 List.of(new DismissAction(i18n))));
    } else if (throwable instanceof KnownLoginErrorException loginException) {
      notificationService.addImmediateErrorNotification(throwable, loginException.getI18nKey());
    } else {
      log.error("Could not log in", throwable);
      notificationService.addImmediateErrorNotification(throwable, "login.failed");
    }

    showLoginForm();
    return null;
  }

  private void loginWithToken() {
    showLoginProgress();
    loginService.loginWithRefreshToken().toFuture()
        .exceptionally(throwable -> {
          throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
          onLoginFailed(throwable);
          showLoginForm();

          log.error("Could not log in with refresh token", throwable);
          return null;
        });
  }

  private void showLoginForm() {
    fxApplicationThreadExecutor.execute(() -> {
      loginFormPane.setVisible(true);
      loginProgressPane.setVisible(false);
      loginButton.setVisible(true);
    });
  }

  private void showLoginProgress() {
    fxApplicationThreadExecutor.execute(() -> {
      loginFormPane.setVisible(false);
      loginProgressPane.setVisible(true);
      loginButton.setVisible(false);
    });
  }

  public void onPlayOfflineButtonClicked() {
    gameRunner.startOffline();
  }

  @Override
  public Pane getRoot() {
    return loginRoot;
  }

  public void onMouseClicked(MouseEvent event) {
    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
      serverConfigPane.setVisible(!serverConfigPane.isVisible());
    }
  }
}
