package com.faforever.client.api;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.login.NoRefreshTokenException;
import com.faforever.client.login.TokenRetrievalException;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.event.LoggedOutEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Service
@Slf4j
public class TokenService implements InitializingBean {
  private final ClientProperties clientProperties;
  private final PreferencesService preferencesService;
  private final EventBus eventBus;
  private final WebClient webClient;
  private final LoginPrefs loginPrefs;
  private final Mono<String> refreshedTokenMono;
  private final Mono<OAuth2AccessToken> tokenRetrievalMono;
  private final Sinks.Many<Long> logoutSink;

  private String refreshTokenValue;
  private MultiValueMap<String, String> hydraPropertiesMap;

  public TokenService(ClientProperties clientProperties, PreferencesService preferencesService, EventBus eventBus, WebClient.Builder webClientBuilder) {
    this.clientProperties = clientProperties;
    this.preferencesService = preferencesService;
    this.eventBus = eventBus;

    loginPrefs = preferencesService.getPreferences().getLogin();
    webClient = webClientBuilder.build();
    logoutSink = Sinks.many().multicast().directBestEffort();

    tokenRetrievalMono = Mono.defer(this::retrieveToken)
        .cacheInvalidateWhen(token ->
            Mono.firstWithSignal(
                Mono.delay(Duration.ofSeconds(token.getExpiresIn() - 30)),
                logoutSink.asFlux().next()
            ).then())
        .doOnNext(token -> log.debug("Token still valid for {} seconds", token.getExpiresIn()));

    refreshedTokenMono = Mono.defer(this::refreshAccess)
        .map(OAuth2AccessToken::getValue)
        .doOnError(throwable -> {
          log.warn("Could not log in with token", throwable);
          loginPrefs.setRefreshToken(null);
          refreshTokenValue = null;
          eventBus.post(new SessionExpiredEvent());
        });
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    eventBus.register(this);
  }

  public Mono<String> getRefreshedTokenValue() {
    return refreshedTokenMono;
  }

  public Mono<Void> loginWithAuthorizationCode(String code) {
    return Mono.fromRunnable(() -> {
          MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
          Oauth oauth = clientProperties.getOauth();
          map.add("code", code);
          map.add("client_id", oauth.getClientId());
          map.add("redirect_uri", oauth.getRedirectUrl());
          map.add("grant_type", "authorization_code");
          hydraPropertiesMap = map;
        })
        .then(tokenRetrievalMono)
        .then();
  }

  public Mono<Void> loginWithRefreshToken() {
    refreshTokenValue = loginPrefs.getRefreshToken();
    return refreshedTokenMono.then();
  }

  private Mono<OAuth2AccessToken> refreshAccess() {
    if (refreshTokenValue == null) {
      loginPrefs.setRefreshToken(null);
      preferencesService.storeInBackground();
      return Mono.error(new NoRefreshTokenException("No refresh token to log in with"));
    }

    return Mono.fromRunnable(() -> {
          Oauth oauth = clientProperties.getOauth();
          MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
          map.add("refresh_token", refreshTokenValue);
          map.add("client_id", oauth.getClientId());
          map.add("redirect_uri", oauth.getRedirectUrl());
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
        .bodyToMono(OAuth2AccessToken.class)
        .doOnSubscribe(subscription -> log.debug("Retrieving OAuth token"))
        .switchIfEmpty(Mono.fromCallable(() -> {
          loginPrefs.setRefreshToken(null);
          refreshTokenValue = null;
          throw new TokenRetrievalException("Could not login with provided parameters");
        }))
        .doOnNext(token -> {
          OAuth2RefreshToken refreshToken = token.getRefreshToken();
          refreshTokenValue = refreshToken != null ? refreshToken.getValue() : null;
          loginPrefs.setRefreshToken(loginPrefs.isRememberMe() ? refreshTokenValue : null);
          preferencesService.storeInBackground();
        });
  }

  @Subscribe
  public void onLogOut(LoggedOutEvent event) {
    logoutSink.tryEmitNext(0L);
  }
}
