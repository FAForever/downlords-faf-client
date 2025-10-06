package com.faforever.client.user;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.TokenRetriever;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.commons.api.dto.MeResult;
import com.faforever.commons.lobby.Player;
import com.google.common.hash.Hashing;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Encoder;

@Lazy
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginService implements InitializingBean {

  private static final Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();

  private final ReadOnlyBooleanWrapper loggedIn = new ReadOnlyBooleanWrapper(false);
  private final ReadOnlyObjectWrapper<MeResult> ownUser = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyObjectWrapper<Player> ownPlayer = new ReadOnlyObjectWrapper<>();

  private final ClientProperties clientProperties;
  private final FafServerAccessor fafServerAccessor;
  private final FafApiAccessor fafApiAccessor;
  private final TokenRetriever tokenRetriever;
  private final NotificationService notificationService;
  private final LoginPrefs loginPrefs;

  @Override
  public void afterPropertiesSet() throws Exception {
    tokenRetriever.invalidationFlux().doOnNext(_ -> {
      if (loggedIn.get()) {
        notificationService.addImmediateInfoNotification("session.expired.message");
        logOut();
      }
    }).doOnError(throwable -> log.error("Error invalidation", throwable)).retry().subscribe();
  }

  public String getHydraUrl(String state, String codeVerifier, URI redirectUri) {
    Oauth oauth = clientProperties.getOauth();
    String scopes = URLEncoder.encode(oauth.getScopes(), StandardCharsets.UTF_8);
    String codeChallenge = BASE64_ENCODER.encodeToString(Hashing.sha256()
        .hashString(codeVerifier, StandardCharsets.US_ASCII)
        .asBytes());

    return UriComponentsBuilder.fromUriString(oauth.getBaseUrl())
                               .path("/oauth2/auth")
                               .queryParam("response_type", "code")
                               .queryParam("client_id", oauth.getClientId())
                               .queryParam("state", state)
                               .queryParam("redirect_uri", redirectUri.toASCIIString())
                               .queryParam("scope", scopes)
                               .queryParam("code_challenge_method", "S256")
                               .queryParam("code_challenge", codeChallenge)
                               .build()
                               .toUriString();
  }

  public Mono<Void> login(String code, String codeVerifier, URI redirectUri) {
    log.info("Logging in with authorization code");
    return tokenRetriever.loginWithAuthorizationCode(code, codeVerifier, redirectUri).then(loginToServices());
  }

  public Mono<Void> loginWithRefreshToken() {
    log.info("Logging in with refresh token");
    return tokenRetriever.loginWithRefreshToken().then(loginToServices());
  }

  private Mono<Void> loginToServices() {
    return Mono.zip(loginToApi(), loginToLobbyServer()).doOnNext(TupleUtils.consumer((meResult, mePlayer) -> {
      if (Integer.parseInt(meResult.getUserId()) != mePlayer.getId()) {
        throw new IllegalStateException("Different player logged into server and api");
      }

      ownUser.set(meResult);
      ownPlayer.set(mePlayer);
    })).doOnError(throwable -> resetLoginState()).then(Mono.fromRunnable(() -> loggedIn.set(true)));
  }

  private Mono<MeResult> loginToApi() {
    return Mono.fromRunnable(fafApiAccessor::authorize).then(Mono.defer(fafApiAccessor::getMe));
  }

  private Mono<Player> loginToLobbyServer() {
    ConnectionState lobbyConnectionState = fafServerAccessor.getConnectionState();
    if (lobbyConnectionState == ConnectionState.CONNECTED) {
      return Mono.just(ownPlayer.get());
    }
    return fafServerAccessor.connectAndLogIn();
  }

  public String getUsername() {
    return getOwnUser().getUserName();
  }

  public Integer getUserId() {
    return Integer.parseInt(getOwnUser().getUserId());
  }

  public void logOut() {
    log.info("Logging out");
    resetLoginState();
  }

  private void resetLoginState() {
    loginPrefs.setRememberMe(false);
    fafApiAccessor.reset();
    fafServerAccessor.disconnect();
    ownUser.set(null);
    ownPlayer.set(null);
    loggedIn.set(false);
    tokenRetriever.invalidateToken();
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
    fafServerAccessor.reconnect().subscribe(myPlayer -> {
      if (myPlayer.getId() != Integer.parseInt(getOwnUser().getUserId())) {
        throw new IllegalStateException("Logged in as unexpected user");
      }
    }, throwable -> {
      log.error("Could not reconnect to server", throwable);
      notificationService.addImmediateErrorNotification(throwable, "login.failed");
      logOut();
    });
  }

  public boolean isLoggedIn() {
    return loggedIn.get();
  }

  public ReadOnlyBooleanProperty loggedInProperty() {
    return loggedIn.getReadOnlyProperty();
  }
}
