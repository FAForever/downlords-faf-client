package com.faforever.client.login;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Irc;
import com.faforever.client.config.ClientProperties.Replay;
import com.faforever.client.config.ClientProperties.Server;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.update.ClientConfiguration.ServerEndpoints;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.update.DownloadUpdateTask;
import com.faforever.client.update.UpdateInfo;
import com.faforever.client.update.Version;
import com.faforever.client.user.UserService;
import com.google.common.annotations.VisibleForTesting;
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
import javafx.scene.web.WebView;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class LoginController implements Controller<Pane> {

  private final UserService userService;
  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final PlatformService platformService;
  private final ClientProperties clientProperties;
  private final I18n i18n;
  private final ClientUpdateService clientUpdateService;
  private final WebViewConfigurer webViewConfigurer;
  private CompletableFuture<Void> initializeFuture;

  public Pane errorPane;
  public Pane loginFormPane;
  public Pane loginProgressPane;
  public WebView loginWebView;
  public ComboBox<ServerEndpoints> environmentComboBox;
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
  public TextField oauthBaseUrlField;
  public Button serverStatusButton;
  public CheckBox rememberMeCheckBox;

  @VisibleForTesting
  CompletableFuture<UpdateInfo> updateInfoFuture;
  private CompletableFuture<Void> resetPageFuture;

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(downloadUpdateButton, loginErrorLabel, loginFormPane, loginWebView,
        serverConfigPane, serverStatusButton, errorPane, loginProgressPane);
    LoginPrefs loginPrefs = preferencesService.getPreferences().getLogin();
    updateInfoFuture = clientUpdateService.getNewestUpdate();

    downloadUpdateButton.setVisible(false);
    errorPane.setVisible(false);
    loginErrorLabel.setVisible(false);
    serverConfigPane.setVisible(false);
    serverStatusButton.setVisible(clientProperties.getStatusPageUrl() != null);
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
          }).thenRunAsync(() -> {
            String refreshToken = loginPrefs.getRefreshToken();
            if (refreshToken != null) {
              loginWithToken(refreshToken);
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
  }

  private void reloadLogin() {
    resetPageFuture = new CompletableFuture<>();
    resetPageFuture.thenAccept(aVoid -> JavaFxUtil.runLater(() -> loginWebView.getEngine().load(userService.getHydraUrl())));
    if (!loginWebView.getEngine().getLoadWorker().isRunning()) {
      resetPageFuture.complete(null);
    }
  }

  private void showClientOutdatedPane(String minimumVersion) {
    JavaFxUtil.runLater(() -> {
      errorPane.setVisible(true);
      loginErrorLabel.setText(i18n.get("login.clientTooOldError", Version.getCurrentVersion(), minimumVersion));
      loginErrorLabel.setVisible(true);
      downloadUpdateButton.setVisible(true);
      loginFormPane.setDisable(true);
      loginFormPane.setVisible(false);
      loginWebView.setVisible(false);
      log.warn("Update required");
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
          showLoginForm();
          notificationService.addImmediateErrorNotification(throwable, "login.failed");
          return null;
        });
  }

  private void loginWithToken(String refreshToken) {
    showLoginProgess();
    userService.loginWithRefreshToken(refreshToken)
        .exceptionally(throwable -> {
          showLoginForm();
          if (!(throwable.getCause() instanceof HttpClientErrorException.BadRequest
              || throwable.getCause() instanceof HttpClientErrorException.Unauthorized)) {
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

  public Pane getRoot() {
    return loginRoot;
  }

  public void onMouseClicked(MouseEvent event) {
    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
      serverConfigPane.setVisible(!serverConfigPane.isVisible());
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
