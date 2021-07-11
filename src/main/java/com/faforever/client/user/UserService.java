package com.faforever.client.user;

import com.faforever.client.api.SessionExpiredEvent;
import com.faforever.client.api.TokenService;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.PlayerInfo;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.commons.api.dto.MeResult;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Lazy
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements InitializingBean, DisposableBean {

  private final ObjectProperty<MeResult> ownUser = new SimpleObjectProperty<>();
  private final ObjectProperty<PlayerInfo> ownPlayerInfo = new SimpleObjectProperty<>();

  private final ClientProperties clientProperties;
  private final FafService fafService;
  private final PreferencesService preferencesService;
  private final EventBus eventBus;
  private final TokenService tokenService;
  private final NotificationService notificationService;
  private final I18n i18n;

  @Getter
  private String state;

  public String getHydraUrl() {
    state = RandomStringUtils.randomAlphanumeric(50, 100);
    Oauth oauth = clientProperties.getOauth();
    return String.format("%s/oauth2/auth?response_type=code&client_id=%s" +
            "&state=%s&redirect_uri=%s" +
            "&scope=%s",
        oauth.getBaseUrl(), oauth.getClientId(), state, oauth.getRedirectUrl(), oauth.getScopes());
  }

  public CompletableFuture<Void> login(String code) {
    return CompletableFuture.runAsync(() -> tokenService.loginWithAuthorizationCode(code))
        .whenComplete((aVoid, throwable) -> {
          if (throwable != null) {
            log.warn("Could not log into the user service with code", throwable);
          }
        })
        .thenCompose(aVoid -> loginToServices());
  }

  public CompletableFuture<Void> loginWithRefreshToken(String refreshToken) {
    return CompletableFuture.runAsync(() -> tokenService.loginWithRefreshToken(refreshToken))
        .whenComplete((aVoid, throwable) -> {
          if (throwable != null) {
            log.info("Could not log into the user service with refresh token", throwable);
          }
        })
        .thenCompose(aVoid -> loginToServices());
  }

  private CompletableFuture<Void> loginToServices() {
    return loginToApi().thenCompose(aVoid -> loginToLobbyServer()).thenRun(() -> eventBus.post(new LoginSuccessEvent()));
  }

  private CompletableFuture<Void> loginToApi() {
    return CompletableFuture.runAsync(fafService::authorizeApi)
        .thenCompose(aVoid -> fafService.getCurrentUser())
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
    ConnectionState lobbyConnectionState = fafService.getLobbyConnectionState();
    if (lobbyConnectionState == ConnectionState.CONNECTED || lobbyConnectionState == ConnectionState.CONNECTING) {
      return CompletableFuture.completedFuture(null);
    }
    return fafService.connectToServer(tokenService.getRefreshedTokenValue())
        .handle((loginMessage, throwable) -> {
          if (throwable != null) {
            log.error("Could not log into the server", throwable);
            throw new CompletionException(throwable);
          }
          if (loginMessage.getMe().getId() != getUserId()) {
            log.error("Player id from server `{}` does not match player id from api `{}`", loginMessage.getMe().getId(), getUserId());
            throw new IllegalStateException("Player id returned by server does not match player id from api");
          }
          ownPlayerInfo.set(loginMessage.getMe());
          return null;
        });
  }

  public String getUsername() {
    return getOwnUser().getUserName();
  }

  public Integer getUserId() {
    return Integer.parseInt(getOwnUser().getUserId());
  }

  @Override
  public void destroy() {
    preferencesService.getPreferences().getLogin().setRefreshToken(tokenService.getRefreshToken());
    preferencesService.storeInBackground();
  }

  private void logOut() {
    log.info("Logging out");
    LoginPrefs loginPrefs = preferencesService.getPreferences().getLogin();
    loginPrefs.setRefreshToken(null);
    loginPrefs.setRememberMe(false);
    preferencesService.storeInBackground();
    fafService.disconnect();
    ownUser.set(null);
    ownPlayerInfo.set(null);
    eventBus.post(new LoggedOutEvent());
  }

  @Subscribe
  public void onSessionExpired(SessionExpiredEvent sessionExpiredEvent) {
    notificationService.addNotification(new ImmediateNotification(i18n.get("session.expired.title"), i18n.get("session.expired.message"), Severity.INFO));
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

  public PlayerInfo getOwnPlayerInfo() {
    return ownPlayerInfo.get();
  }
}
