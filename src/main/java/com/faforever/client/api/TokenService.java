package com.faforever.client.api;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.login.NoRefreshTokenException;
import com.faforever.client.login.TokenRetrievalException;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.user.event.LoggedOutEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.time.Duration;

@Service
@Slf4j
public class TokenService implements InitializingBean {
  private final ClientProperties clientProperties;
  private final EventBus eventBus;
  private final WebClient webClient;
  private final LoginPrefs loginPrefs;

  private final Sinks.Many<Long> logoutSink = Sinks.many().multicast().directBestEffort();
  private final Mono<OAuth2AccessToken> tokenRetrievalMono = Mono.defer(this::retrieveToken)
      .cacheInvalidateWhen(this::getTokenExpirartionMono)
      .doOnNext(token -> log.trace("Token still valid for {} seconds", token.getExpiresIn()));
  private final Mono<String> refreshedTokenMono = Mono.defer(this::refreshAccess)
      .map(OAuth2AccessToken::getValue)
      .doOnError(this::onTokenError);


  private String refreshTokenValue;
  private MultiValueMap<String, String> hydraPropertiesMap;

  public TokenService(ClientProperties clientProperties, EventBus eventBus, Builder webClientBuilder, LoginPrefs loginPrefs) {
    this.clientProperties = clientProperties;
    this.eventBus = eventBus;

    webClient = webClientBuilder.build();
    this.loginPrefs = loginPrefs;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    eventBus.register(this);
  }

  public Mono<String> getRefreshedTokenValue() {
    return refreshedTokenMono;
  }

  public Mono<Void> loginWithAuthorizationCode(String code, String codeVerifier, URI redirectUri) {
    return Mono.fromRunnable(() -> {
          MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
          Oauth oauth = clientProperties.getOauth();
          map.add("code", code);
          map.add("client_id", oauth.getClientId());
          map.add("redirect_uri", redirectUri.toASCIIString());
          map.add("grant_type", "authorization_code");
          map.add("code_verifier", codeVerifier);
          hydraPropertiesMap = map;
        })
        .then(tokenRetrievalMono)
        .then();
  }

  public Mono<Void> loginWithRefreshToken() {
    refreshTokenValue = loginPrefs.getRefreshToken();
    return refreshedTokenMono.then();
  }

  private void onTokenError(Throwable throwable) {
    log.warn("Could not log in with token", throwable);
    loginPrefs.setRefreshToken(null);
    refreshTokenValue = null;
    eventBus.post(new SessionExpiredEvent());
  }

  private Mono<Void> getTokenExpirartionMono(OAuth2AccessToken token) {
    return Mono.firstWithSignal(
        Mono.delay(Duration.ofSeconds(token.getExpiresIn() - 30)),
        logoutSink.asFlux().next()
    ).then();
  }

  private Mono<OAuth2AccessToken> refreshAccess() {
    if (refreshTokenValue == null) {
      loginPrefs.setRefreshToken(null);
      return Mono.error(new NoRefreshTokenException("No refresh token to log in with"));
    }

    return Mono.fromRunnable(() -> {
          Oauth oauth = clientProperties.getOauth();
          MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
          map.add("refresh_token", refreshTokenValue);
          map.add("client_id", oauth.getClientId());
          map.add("grant_type", "refresh_token");
          hydraPropertiesMap = map;
        })
        .then(tokenRetrievalMono);
  }

  private Mono<OAuth2AccessToken> retrieveToken() {
    return webClient.post()
        .uri(String.format("%s/oauth2/token", clientProperties.getOauth().getBaseUrl()))
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(hydraPropertiesMap)
        .retrieve()
        .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class).flatMap(body -> Mono.error(new TokenRetrievalException(body))))
        .bodyToMono(OAuth2AccessToken.class)
        .doOnSubscribe(subscription -> log.trace("Retrieving OAuth token"))
        .switchIfEmpty(Mono.fromCallable(() -> {
          loginPrefs.setRefreshToken(null);
          refreshTokenValue = null;
          throw new TokenRetrievalException("Could not login with provided parameters");
        }))
        .doOnNext(token -> {
          OAuth2RefreshToken refreshToken = token.getRefreshToken();
          refreshTokenValue = refreshToken != null ? refreshToken.getValue() : null;
          loginPrefs.setRefreshToken(loginPrefs.isRememberMe() ? refreshTokenValue : null);
        });
  }

  @Subscribe
  public void onLogOut(LoggedOutEvent event) {
    logoutSink.tryEmitNext(0L);
  }
}
