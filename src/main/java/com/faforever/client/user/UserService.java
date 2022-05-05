package com.faforever.client.user;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.SessionExpiredEvent;
import com.faforever.client.api.TokenService;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.commons.api.dto.MeResult;
import com.faforever.commons.lobby.Player;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.hash.Hashing;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Lazy
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements InitializingBean {

  private static final Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();

  private final ReadOnlyObjectWrapper<MeResult> ownUser = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyObjectWrapper<Player> ownPlayer = new ReadOnlyObjectWrapper<>();

  private final ClientProperties clientProperties;
  private final FafServerAccessor fafServerAccessor;
  private final FafApiAccessor fafApiAccessor;
  private final PreferencesService preferencesService;
  private final EventBus eventBus;
  private final TokenService tokenService;
  private final NotificationService notificationService;

  private CompletableFuture<Void> loginFuture;

  public String getHydraUrl(URI redirectUri, String state, String codeVerifier) {
    Oauth oauth = clientProperties.getOauth();
    String codeChallenge = BASE64_ENCODER.encodeToString(Hashing.sha256().hashString(codeVerifier, StandardCharsets.US_ASCII).asBytes());
    return String.format("%s/oauth2/auth?response_type=code&client_id=%s&state=%s&redirect_uri=%s&scope=%s&code_challenge_method=S256&code_challenge=%s",
        oauth.getBaseUrl(), oauth.getClientId(), state, redirectUri.toASCIIString(), oauth.getScopes(), codeChallenge);
  }

  public CompletableFuture<Void> login(String code, URI redirectUri, String codeVerifier) {
    if (loginFuture == null || loginFuture.isDone()) {
      log.info("Logging in with authorization code");
      loginFuture = tokenService.loginWithAuthorizationCode(code, redirectUri, codeVerifier).toFuture()
          .thenCompose(aVoid -> loginToServices());
    }
    return loginFuture;
  }

  public CompletableFuture<Void> loginWithRefreshToken() {
    if (loginFuture == null || loginFuture.isDone()) {
      log.info("Logging in with refresh token");
      loginFuture = tokenService.loginWithRefreshToken().toFuture()
          .thenCompose(aVoid -> loginToServices());
    }
    return loginFuture;
  }

  private CompletableFuture<Void> loginToServices() {
    return loginToApi().thenCompose(aVoid -> loginToLobbyServer()).thenRun(() -> eventBus.post(new LoginSuccessEvent()));
  }

  private CompletableFuture<Void> loginToApi() {
    return CompletableFuture.runAsync(fafApiAccessor::authorize)
        .thenCompose(aVoid -> fafApiAccessor.getMe().toFuture())
        .thenAccept(me -> {
          if (getOwnUser() == null) {
            ownUser.set(me);
          } else if (getOwnUser() != me) {
            logOut();
            ownUser.set(me);
          }
        }).whenComplete((aVoid, throwable) -> {
          if (throwable != null) {
            log.error("Could not log into the api", throwable);
          }
        });
  }

  private CompletableFuture<Void> loginToLobbyServer() {
    ConnectionState lobbyConnectionState = fafServerAccessor.getConnectionState();
    if (lobbyConnectionState == ConnectionState.CONNECTED || lobbyConnectionState == ConnectionState.CONNECTING) {
      return CompletableFuture.completedFuture(null);
    }
    return fafServerAccessor.connectAndLogIn()
        .handle((loginMessage, throwable) -> {
          if (throwable != null) {
            log.error("Could not log into the server", throwable);
            throw new CompletionException(throwable);
          }
          if (loginMessage.getMe().getId() != getUserId()) {
            log.error("Player id from server `{}` does not match player id from api `{}`", loginMessage.getMe().getId(), getUserId());
            throw new IllegalStateException("Player id returned by server does not match player id from api");
          }
          ownPlayer.set(loginMessage.getMe());
          return null;
        });
  }

  public String getUsername() {
    return getOwnUser().getUserName();
  }

  public Integer getUserId() {
    return Integer.parseInt(getOwnUser().getUserId());
  }

  private void logOut() {
    log.info("Logging out");
    LoginPrefs loginPrefs = preferencesService.getPreferences().getLogin();
    loginPrefs.setRefreshToken(null);
    preferencesService.storeInBackground();
    fafServerAccessor.disconnect();
    ownUser.set(null);
    ownPlayer.set(null);
    eventBus.post(new LoggedOutEvent());
  }

  @Subscribe
  public void onSessionExpired(SessionExpiredEvent sessionExpiredEvent) {
    notificationService.addImmediateInfoNotification("session.expired.message");
  }

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  @Subscribe
  public void onLogoutRequestEvent(LogOutRequestEvent event) {
    logOut();
  }

  public MeResult getOwnUser() {
    return ownUser.get();
  }

  public Player getOwnPlayer() {
    return ownPlayer.get();
  }

  public ReadOnlyObjectProperty<Player> ownPlayerProperty() {
    return ownPlayer.getReadOnlyProperty();
  }

  public ConnectionState getConnectionState() {
    return fafServerAccessor.getConnectionState();
  }

  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return fafServerAccessor.connectionStateProperty();
  }

  public void reconnectToLobby() {
    fafServerAccessor.reconnect();
  }
}
