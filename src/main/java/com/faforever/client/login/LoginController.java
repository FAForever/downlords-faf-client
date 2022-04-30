package com.faforever.client.login;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Irc;
import com.faforever.client.config.ClientProperties.Replay;
import com.faforever.client.config.ClientProperties.Server;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.status.Message;
import com.faforever.client.status.Service;
import com.faforever.client.status.StatPingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.update.ClientConfiguration.ServerEndpoints;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.update.DownloadUpdateTask;
import com.faforever.client.update.UpdateInfo;
import com.faforever.client.update.Version;
import com.faforever.client.user.UserService;
import com.faforever.client.util.ConcurrentUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.Node;
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
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class LoginController implements Controller<Pane> {

  private final GameService gameService;
  private final UserService userService;
  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final ClientProperties clientProperties;
  private final I18n i18n;
  private final ClientUpdateService clientUpdateService;
  private final StatPingService statPingService;
  private final UiService uiService;
  private final PlatformService platformService;
  private final OAuthValuesReceiver oAuthValuesReceiver;

  private CompletableFuture<Void> initializeFuture;

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
  public TextField serverHostField;
  public TextField serverPortField;
  public TextField replayServerHostField;
  public TextField replayServerPortField;
  public TextField ircServerHostField;
  public TextField ircServerPortField;
  public TextField apiBaseUrlField;
  public TextField oauthBaseUrlField;
  public TextField oauthRedirectUriField;
  public CheckBox rememberMeCheckBox;

  @VisibleForTesting
  CompletableFuture<UpdateInfo> updateInfoFuture;

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(downloadUpdateButton, loginErrorLabel, loginFormPane,
        serverConfigPane, errorPane, loginProgressPane, messagesContainer, loginButton);
    LoginPrefs loginPrefs = preferencesService.getPreferences().getLogin();
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

    ReadOnlyObjectProperty<ServerEndpoints> selectedEndpointProperty = environmentComboBox.getSelectionModel().selectedItemProperty();

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
    JavaFxUtil.addListener(
        oauthRedirectUriField.textProperty(),
        observable -> clientProperties.getOauth().setRedirectUri(URI.create(oauthRedirectUriField.getText()))
    );

    if (clientProperties.isUseRemotePreferences()) {
      initializeFuture = preferencesService.getRemotePreferencesAsync()
          .thenApply(clientConfiguration -> {
            ServerEndpoints defaultEndpoint = clientConfiguration.getEndpoints().get(0);

            clientProperties.updateFromEndpoint(defaultEndpoint);

            String minimumVersion = clientConfiguration.getLatestRelease().getMinimumVersion();
            boolean shouldUpdate = false;
            if (minimumVersion != null) {
              shouldUpdate = Version.shouldUpdate(Version.getCurrentVersion(), minimumVersion);

              if (shouldUpdate) {
                JavaFxUtil.runLater(() -> showClientOutdatedPane(minimumVersion));
              }
            }

            JavaFxUtil.runLater(() -> {
              environmentComboBox.getItems().addAll(clientConfiguration.getEndpoints());
              environmentComboBox.getSelectionModel().select(defaultEndpoint);
            });
            return shouldUpdate;
          }).exceptionally(throwable -> {
            log.error("Could not read remote preferences", throwable);
            return false;
          }).thenAccept(shouldUpdate -> {
            if (!shouldUpdate && loginPrefs.isRememberMe() && loginPrefs.getRefreshToken() != null) {
              loginWithToken();
            }
          });
    } else {
      initializeFuture = CompletableFuture.completedFuture(null);
    }

    checkServiceStatus();
  }

  private void checkServiceStatus() {
    checkGlobalAnnouncements().thenAccept(messages -> {
      displayAnnouncements(messages);

      // Only check for offline services if no announced maintenance is happening right now. Otherwise, the user
      // might see redundant messages.
      if (messages.stream().noneMatch(this::isHappeningNow)) {
        checkOfflineServices().thenAccept(this::displayOfflineServices);
      }
    });
  }

  private boolean isHappeningNow(Message message) {
    OffsetDateTime now = OffsetDateTime.now();
    return message.getStartOn().isBefore(now) && message.getEndOn().isAfter(now);
  }

  private CompletableFuture<List<Message>> checkGlobalAnnouncements() {
    return statPingService.getMessages()
        .filter(LoginController::shouldDisplayAnnouncement)
        .collectList().toFuture();
  }

  private static boolean shouldDisplayAnnouncement(Message message) {
    return message.getEndOn().isAfter(OffsetDateTime.now());
  }

  private CompletableFuture<List<Service>> checkOfflineServices() {
    return statPingService.getServices()
        .filter(service -> !service.isOnline())
        .collectList().toFuture();
  }

  private void displayAnnouncements(List<Message> messages) {
    if (messages.isEmpty()) {
      return;
    }
    List<Node> controllers = messages.stream().map(message -> {
      AnnouncementController controller = uiService.loadFxml("theme/login/announcement.fxml");
      controller.setTitle(message.getTitle());
      controller.setMessage(message.getDescription());
      controller.setTime(message.getStartOn(), message.getEndOn());
      return controller.getRoot();
    }).collect(Collectors.toList());
    JavaFxUtil.runLater(() -> {
      messagesContainer.getChildren().addAll(controllers);
      messagesContainer.setVisible(true);
    });
  }

  private void displayOfflineServices(List<Service> services) {
    if (services.isEmpty()) {
      return;
    }

    OfflineServicesController controller = uiService.loadFxml("theme/login/offline_services.fxml");
    services.forEach(service -> controller.addService(service.getName(), findOfflineReason(service), service.getLastSuccess()));
    JavaFxUtil.runLater(() -> {
      messagesContainer.getChildren().add(controller.getRoot());
      messagesContainer.setVisible(true);
    });
  }

  private String findOfflineReason(Service service) {
    if (!service.getIncidents().isEmpty()) {
      return service.getIncidents().get(0).getTitle();
    }

    return service.getMessages().stream()
        .filter(this::isHappeningNow)
        .findFirst()
        .map(Message::getTitle)
        .orElse(i18n.get("reasonUnknown"));
  }

  private void showClientOutdatedPane(String minimumVersion) {
    log.info("Client Update required");
    JavaFxUtil.runLater(() -> {
      errorPane.setVisible(true);
      loginErrorLabel.setText(i18n.get("login.clientTooOldError", Version.getCurrentVersion(), minimumVersion));
      loginErrorLabel.setVisible(true);
      downloadUpdateButton.setVisible(true);
      loginFormPane.setDisable(true);
      loginFormPane.setVisible(false);
      loginButton.setVisible(false);
      playOfflineButton.setVisible(false);
    });
  }

  private void populateEndpointFields() {
    JavaFxUtil.runLater(() -> {
      Server server = clientProperties.getServer();
      serverHostField.setText(server.getHost());
      serverPortField.setText(String.valueOf(server.getPort()));
      Replay replay = clientProperties.getReplay();
      replayServerHostField.setText(replay.getRemoteHost());
      replayServerPortField.setText(String.valueOf(replay.getRemotePort()));
      Irc irc = clientProperties.getIrc();
      ircServerHostField.setText(irc.getHost());
      ircServerPortField.setText(String.valueOf(irc.getPort()));
      apiBaseUrlField.setText(clientProperties.getApi().getBaseUrl());
      oauthBaseUrlField.setText(clientProperties.getOauth().getBaseUrl());
      oauthRedirectUriField.setText(clientProperties.getOauth().getRedirectUri().toASCIIString());
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
            downloadUpdateButton.textProperty().bind(
                Bindings.createStringBinding(() -> downloadUpdateTask.getProgress() == -1 ?
                        i18n.get("login.button.downloadPreparing") :
                        i18n.get("login.button.downloadProgress", downloadUpdateTask.getProgress()),
                    downloadUpdateTask.progressProperty()));
          }
        });
  }

  public CompletableFuture<Void> onLoginButtonClicked() {
    initializeFuture.join();

    clientProperties.getServer()
        .setHost(serverHostField.getText())
        .setPort(Integer.parseInt(serverPortField.getText()));

    clientProperties.getReplay()
        .setRemoteHost(replayServerHostField.getText())
        .setRemotePort(Integer.parseInt(replayServerPortField.getText()));

    clientProperties.getIrc()
        .setHost(ircServerHostField.getText())
        .setPort(Integer.parseInt(ircServerPortField.getText()));

    clientProperties.getApi().setBaseUrl(apiBaseUrlField.getText());
    clientProperties.getOauth().setBaseUrl(oauthBaseUrlField.getText());

    List<URI> redirectUriCandidates = new ArrayList<>();

    if (!oauthRedirectUriField.getText().isBlank()) {
      redirectUriCandidates.add(URI.create(oauthRedirectUriField.getText()));
    }

    ServerEndpoints endpoint = environmentComboBox.getValue();

    if (endpoint != null) {
      redirectUriCandidates.addAll(endpoint.getOauth().getRedirectUris());
    }

    return oAuthValuesReceiver.receiveValues(redirectUriCandidates)
        .thenCompose(values -> {
          platformService.focusWindow(clientProperties.getMainWindowTitle());
          String actualState = values.getState();
          String expectedState = userService.getState();
          if (!expectedState.equals(actualState)) {
            handleInvalidSate(actualState, expectedState);
            return CompletableFuture.completedFuture(null);
          }
          return loginWithCode(values.getCode(), values.getRedirectUri());
        })
        .exceptionally(throwable -> onLoginFailed(ConcurrentUtil.unwrapIfCompletionException(throwable)));
  }

  private void handleInvalidSate(String actualState, String expectedState) {
    showLoginForm();
    log.warn("Reported state does not match. Expected `{}` but got `{}`", expectedState, actualState);
    notificationService.addImmediateErrorNotification(
        new IllegalStateException("State returned by the server does not match expected state"),
        "login.failed"
    );
  }

  private CompletableFuture<Void> loginWithCode(String code, URI redirectUri) {
    showLoginProgress();
    return userService.login(code, redirectUri);
  }

  private Void onLoginFailed(Throwable throwable) {
    if (userService.getOwnUser() != null && userService.getOwnPlayer() != null) {
      log.info("Previous login request failed but user is already logged in", throwable);
      return null;
    }

    if (throwable instanceof SocketTimeoutException) {
      log.info("Login request timed out", throwable);
      notificationService.addImmediateWarnNotification("login.timeout");
    } else {
      log.error("Could not log in with code", throwable);
      notificationService.addImmediateErrorNotification(throwable, "login.failed");
    }

    showLoginForm();
    return null;
  }

  private void loginWithToken() {
    showLoginProgress();
    userService.loginWithRefreshToken()
        .exceptionally(throwable -> {
          throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
          showLoginForm();

          log.error("Could not log in with refresh token", throwable);
          if (!(throwable instanceof WebClientResponseException.BadRequest || throwable instanceof WebClientResponseException.Unauthorized)) {
            notificationService.addImmediateErrorNotification(throwable, "login.failed");
          }
          return null;
        });
  }

  private void showLoginForm() {
    JavaFxUtil.runLater(() -> {
      loginFormPane.setVisible(true);
      loginProgressPane.setVisible(false);
      loginButton.setVisible(true);
    });
  }

  private void showLoginProgress() {
    JavaFxUtil.runLater(() -> {
      loginFormPane.setVisible(false);
      loginProgressPane.setVisible(true);
      loginButton.setVisible(false);
    });
  }

  public void onPlayOfflineButtonClicked() {
    try {
      gameService.startGameOffline();
    } catch (IOException e) {
      notificationService.addImmediateWarnNotification("offline.noExe");
    }

  }

  public Pane getRoot() {
    return loginRoot;
  }

  public void onMouseClicked(MouseEvent event) {
    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
      serverConfigPane.setVisible(!serverConfigPane.isVisible());
    }
  }
}
