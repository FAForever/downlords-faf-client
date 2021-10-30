package com.faforever.client.login;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Irc;
import com.faforever.client.config.ClientProperties.Replay;
import com.faforever.client.config.ClientProperties.Server;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WebViewConfigurer;
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
import javafx.scene.web.WebView;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
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
  private final WebViewConfigurer webViewConfigurer;
  private final StatPingService statPingService;
  private final UiService uiService;

  private CompletableFuture<Void> initializeFuture;

  public Pane messagesContainer;
  public Pane errorPane;
  public Pane loginFormPane;
  public Pane loginProgressPane;
  public WebView loginWebView;
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
  public CheckBox rememberMeCheckBox;

  @VisibleForTesting
  CompletableFuture<UpdateInfo> updateInfoFuture;
  private CompletableFuture<Void> resetPageFuture;

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(downloadUpdateButton, loginErrorLabel, loginFormPane, loginWebView,
        serverConfigPane, errorPane, loginProgressPane, messagesContainer);
    LoginPrefs loginPrefs = preferencesService.getPreferences().getLogin();
    updateInfoFuture = clientUpdateService.getNewestUpdate();

    messagesContainer.setVisible(false);
    downloadUpdateButton.setVisible(false);
    errorPane.setVisible(false);
    loginErrorLabel.setVisible(false);
    serverConfigPane.setVisible(false);
    rememberMeCheckBox.setSelected(loginPrefs.isRememberMe());
    loginPrefs.rememberMeProperty().bindBidirectional(rememberMeCheckBox.selectedProperty());

    resetPageFuture = new CompletableFuture<>();

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

      loginWebView.getEngine().load(userService.getHydraUrl());
    });

    oauthBaseUrlField.setOnAction((event -> {
      clientProperties.getOauth().setBaseUrl(oauthBaseUrlField.getText());
      loginWebView.getEngine().load(userService.getHydraUrl());
    }));


    if (clientProperties.isUseRemotePreferences()) {
      initializeFuture = preferencesService.getRemotePreferencesAsync()
          .thenAccept(clientConfiguration -> {
            ServerEndpoints defaultEndpoint = clientConfiguration.getEndpoints().get(0);

            clientProperties.updateFromEndpoint(defaultEndpoint);

            String minimumVersion = clientConfiguration.getLatestRelease().getMinimumVersion();
            boolean shouldUpdate = false;
            try {
              shouldUpdate = Version.shouldUpdate(Version.getCurrentVersion(), minimumVersion);
            } catch (Exception e) {
              log.error("Error occurred checking for update", e);
            }

            if (minimumVersion != null && shouldUpdate) {
              JavaFxUtil.runLater(() -> showClientOutdatedPane(minimumVersion));
            }

            JavaFxUtil.runLater(() -> {
              environmentComboBox.getItems().addAll(clientConfiguration.getEndpoints());
              environmentComboBox.getSelectionModel().select(defaultEndpoint);
            });
          }).exceptionally(throwable -> {
            log.warn("Could not read remote preferences", throwable);
            return null;
          }).thenRun(() -> {
            if (loginPrefs.isRememberMe() && loginPrefs.getRefreshToken() != null) {
              loginWithToken();
            }
          });
    } else {
      initializeFuture = CompletableFuture.completedFuture(null);
    }

    webViewConfigurer.configureWebView(loginWebView);

    loginWebView.getEngine().getLoadWorker().runningProperty().addListener(((observable, oldValue, newValue) -> {
      if (!newValue) {
        resetPageFuture.complete(null);
      }
    }));

    loginWebView.getEngine().locationProperty().addListener((observable, oldValue, newValue) -> {
      String location = newValue;

      List<NameValuePair> params;
      try {
        params = URLEncodedUtils.parse(new URI(newValue), StandardCharsets.UTF_8);
      } catch (URISyntaxException e) {
        log.warn("Could not parse webpage url: {}", newValue, e);
        reloadLogin();
        notificationService.addImmediateErrorNotification(e, "login.error");
        return;
      }

      if (params.stream().anyMatch(param -> param.getName().equals("error"))) {
        String error = params.stream().filter(param -> param.getName().equals("error"))
            .findFirst().map(NameValuePair::getValue).orElse(null);
        String errorDescription = params.stream().filter(param -> param.getName().equals("error_description"))
            .findFirst().map(NameValuePair::getValue).orElse(null);
        log.warn("Error during login error: url {}; error {}; {}", newValue, error, errorDescription);
        reloadLogin();
        notificationService.addImmediateErrorNotification(new RuntimeException("Error during login"), "login.error", error, errorDescription);
        return;
      }

      String reportedState = params.stream().filter(param -> param.getName().equals("state"))
          .map(NameValuePair::getValue)
          .findFirst()
          .orElse(null);

      String code = params.stream().filter(param -> param.getName().equals("code"))
          .map(NameValuePair::getValue)
          .findFirst()
          .orElse(null);

      if (reportedState != null) {

        String userServiceState = userService.getState();
        if (!userServiceState.equals(reportedState)) {
          log.warn("Reported state does not match there is something fishy going on. Saved State `{}`, Returned State `{}`, Location `{}`", userServiceState, reportedState, newValue);
          reloadLogin();
          notificationService.addImmediateErrorNotification(new IllegalStateException("State returned by user service does not match initial state"), "login.badState");
          return;
        }

        if (code != null) {
          location = location.replace(code, "*****");

          initializeFuture.join();

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
          clientProperties.getOauth().setBaseUrl(oauthBaseUrlField.getText());

          loginWithCode(code);
        }
      }

      log.debug("login web view visited {}", location);
    });

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

  private void reloadLogin() {
    resetPageFuture = new CompletableFuture<>();
    resetPageFuture.thenAccept(aVoid -> JavaFxUtil.runLater(() -> loginWebView.getEngine().load(userService.getHydraUrl())));
    if (!loginWebView.getEngine().getLoadWorker().isRunning()) {
      resetPageFuture.complete(null);
    }
  }

  private void showClientOutdatedPane(String minimumVersion) {
    log.warn("Client Update required");
    JavaFxUtil.runLater(() -> {
      errorPane.setVisible(true);
      loginErrorLabel.setText(i18n.get("login.clientTooOldError", Version.getCurrentVersion(), minimumVersion));
      loginErrorLabel.setVisible(true);
      downloadUpdateButton.setVisible(true);
      loginFormPane.setDisable(true);
      loginFormPane.setVisible(false);
      loginWebView.setVisible(false);
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

  private void loginWithCode(String code) {
    showLoginProgess();
    userService.login(code)
        .exceptionally(throwable -> {
          log.warn("Could not log in with code", throwable);
          showLoginForm();
          notificationService.addImmediateErrorNotification(throwable, "login.failed");
          return null;
        });
  }

  private void loginWithToken() {
    showLoginProgess();
    userService.loginWithRefreshToken()
        .exceptionally(throwable -> {
          throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
          showLoginForm();

          log.warn("Could not log in with refresh", throwable);
          if (!(throwable instanceof WebClientResponseException.BadRequest || throwable instanceof WebClientResponseException.Unauthorized)) {
            notificationService.addImmediateErrorNotification(throwable, "login.failed");
          }
          return null;
        });
  }

  private void showLoginForm() {
    JavaFxUtil.runLater(() -> {
      loginWebView.getEngine().load(userService.getHydraUrl());
      loginFormPane.setVisible(true);
      loginProgressPane.setVisible(false);
    });

  }

  private void showLoginProgess() {
    JavaFxUtil.runLater(() -> {
      loginFormPane.setVisible(false);
      loginProgressPane.setVisible(true);
    });
  }

  public void onPlayOfflineButtonClicked() {
    try {
      gameService.startGameOffline();
    } catch (Exception e) {
      if (e.getCause() instanceof IOException) {
        notificationService.addImmediateWarnNotification("offline.noExe");
      } else {
        notificationService.addImmediateErrorNotification(e, "offline.error");
      }
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
