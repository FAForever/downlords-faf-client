package com.faforever.client.api;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.login.NoRefreshTokenException;
import com.faforever.client.login.TokenRetrievalException;
import com.faforever.client.preferences.LoginPrefs;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.web.reactive.function.OAuth2BodyExtractors;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;
import reactor.core.publisher.Sinks.Many;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenRetriever implements InitializingBean {
  private final ClientProperties clientProperties;
  private final WebClient defaultWebClient;
  private final LoginPrefs loginPrefs;

  private final Many<Long> invalidateSink = Sinks.many().multicast().directBestEffort();
  private final Flux<Long> invalidateFlux = invalidateSink.asFlux().publish().autoConnect();
  private final StringProperty refreshTokenValue = new SimpleStringProperty();

  private final Mono<String> refreshedTokenMono = Mono.defer(this::refreshAccess)
                                                      .cacheInvalidateWhen(this::getExpirationMono)
                                                      .map(OAuth2AccessToken::getTokenValue);

  @Override
  public void afterPropertiesSet() throws Exception {
    refreshTokenValue.set(loginPrefs.getRefreshToken());
    loginPrefs.refreshTokenProperty()
              .bind(loginPrefs.rememberMeProperty().flatMap(remember -> remember ? refreshTokenValue : null));
  }

  private Mono<Void> getExpirationMono(OAuth2AccessToken token) {
    Mono<Long> invalidationMono = invalidateFlux.next();
    Mono<Long> expirationMono = Mono.delay(Duration.between(Instant.now(), token.getExpiresAt()).minusSeconds(30));
    return Mono.firstWithSignal(invalidationMono, expirationMono).then();
  }

  public Mono<String> getRefreshedTokenValue() {
    return refreshedTokenMono.doOnError(this::onTokenError);
  }

  public Mono<Void> loginWithAuthorizationCode(String code, String codeVerifier, URI redirectUri) {
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    Oauth oauth = clientProperties.getOauth();
    map.add("code", code);
    map.add("client_id", oauth.getClientId());
    map.add("redirect_uri", redirectUri.toASCIIString());
    map.add("grant_type", "authorization_code");
    map.add("code_verifier", codeVerifier);
    return retrieveToken(map).then();
  }

  public Mono<Void> loginWithRefreshToken() {
    return refreshedTokenMono.then();
  }

  private void onTokenError(Throwable throwable) {
    log.warn("Could not retrieve token", throwable);
    invalidateToken();
  }

  private Mono<OAuth2AccessToken> refreshAccess() {
    String refreshToken = refreshTokenValue.get();
    if (refreshToken == null) {
      return Mono.error(new NoRefreshTokenException("No refresh token to log in with"));
    }

    Oauth oauth = clientProperties.getOauth();
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("refresh_token", refreshToken);
    map.add("client_id", oauth.getClientId());
    map.add("grant_type", "refresh_token");

    return retrieveToken(map);
  }

  private Mono<OAuth2AccessToken> retrieveToken(MultiValueMap<String, String> properties) {
    return defaultWebClient.post()
        .uri(String.format("%s/oauth2/token", clientProperties.getOauth().getBaseUrl()))
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(properties)
        .exchangeToMono(response -> {
          if (response.statusCode().isError()) {
            return response.bodyToMono(String.class)
                .switchIfEmpty(Mono.just(response.statusCode().toString()))
                .flatMap(body -> Mono.error(new TokenRetrievalException(body)));
          }

          return response.body(OAuth2BodyExtractors.oauth2AccessTokenResponse());
        })
        .doOnSubscribe(subscription -> log.debug("Retrieving OAuth token"))
        .doOnNext(tokenResponse -> {
          OAuth2RefreshToken refreshToken = tokenResponse.getRefreshToken();
          refreshTokenValue.set(refreshToken != null ? refreshToken.getTokenValue() : null);
        })
        .map(OAuth2AccessTokenResponse::getAccessToken)
        .doOnNext(token -> log.info("Token valid until {}", token.getExpiresAt()));
  }

  public void invalidateToken() {
    refreshTokenValue.set(null);
    invalidateSink.emitNext(0L, EmitFailureHandler.busyLooping(Duration.ofMillis(100)));
  }

  public Flux<Long> invalidationFlux() {
    return invalidateFlux;
  }

  public Mono<String> getAccessToken() {
    return refreshedTokenMono;
  }
}
