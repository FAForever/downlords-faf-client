package com.faforever.client.api;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.login.NoRefreshTokenException;
import com.faforever.client.login.TokenRetrievalException;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.user.event.LoggedOutEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.web.reactive.function.OAuth2BodyExtractors;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService implements InitializingBean {
  private final ClientProperties clientProperties;
  private final EventBus eventBus;
  private final WebClient defaultWebClient;
  private final LoginPrefs loginPrefs;

  private final Sinks.Many<Long> logoutSink = Sinks.many().multicast().directBestEffort();
  private final Mono<OAuth2AccessToken> tokenRetrievalMono = Mono.defer(this::retrieveToken)
      .cacheInvalidateWhen(this::getTokenExpirationMono)
      .doOnNext(token -> log.debug("Token valid until {}", token.getExpiresAt()));
  private final Mono<String> refreshedTokenMono = Mono.defer(this::refreshAccess)
      .map(OAuth2AccessToken::getTokenValue)
      .doOnError(this::onTokenError);


  private String refreshTokenValue;
  private MultiValueMap<String, String> hydraPropertiesMap;

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
    }).then(tokenRetrievalMono).then();
  }

  public Mono<Void> loginWithRefreshToken() {
    refreshTokenValue = loginPrefs.getRefreshToken();
    return refreshedTokenMono.then();
  }

  private void onTokenError(Throwable throwable) {
    log.warn("Could not log in with token", throwable);
    loginPrefs.setRefreshToken(null);
    refreshTokenValue = null;
    hydraPropertiesMap = null;
    eventBus.post(new SessionExpiredEvent());
  }

  private Mono<Void> getTokenExpirationMono(OAuth2AccessToken token) {
    return Mono.firstWithSignal(Mono.delay(Duration.between(Instant.now(), token.getExpiresAt())
        .minusSeconds(30)), logoutSink.asFlux().next()).then();
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
    }).then(tokenRetrievalMono);
  }

  private Mono<OAuth2AccessToken> retrieveToken() {
    return defaultWebClient.post()
        .uri(String.format("%s/oauth2/token", clientProperties.getOauth().getBaseUrl()))
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(hydraPropertiesMap)
        .exchangeToMono(response -> {
          if (response.statusCode().isError()) {
            return response.bodyToMono(String.class)
                .switchIfEmpty(Mono.just(response.statusCode().toString()))
                .flatMap(body -> Mono.error(new TokenRetrievalException(body)));
          }

          return response.body(OAuth2BodyExtractors.oauth2AccessTokenResponse());
        })
        .doOnSubscribe(subscription -> log.debug("Retrieving OAuth token"))
        .doOnError(throwable -> {
          loginPrefs.setRefreshToken(null);
          refreshTokenValue = null;
        })
        .doOnNext(token -> {
          OAuth2RefreshToken refreshToken = token.getRefreshToken();
          refreshTokenValue = refreshToken != null ? refreshToken.getTokenValue() : null;
          loginPrefs.setRefreshToken(loginPrefs.isRememberMe() ? refreshTokenValue : null);
        })
        .map(OAuth2AccessTokenResponse::getAccessToken);
  }

  @Subscribe
  public void onLogOut(LoggedOutEvent event) {
    hydraPropertiesMap = null;
    logoutSink.tryEmitNext(0L);
  }
}
