package com.faforever.client.api;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.login.NoRefreshTokenException;
import com.faforever.client.login.TokenRetrievalException;
import com.faforever.client.preferences.LoginPrefs;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.web.reactive.function.OAuth2BodyExtractors;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenRetriever {
  private final ClientProperties clientProperties;
  private final WebClient defaultWebClient;
  private final LoginPrefs loginPrefs;

  private final StringProperty refreshTokenValue = new SimpleStringProperty();
  private final ReadOnlyBooleanWrapper tokenInvalid = new ReadOnlyBooleanWrapper(true);

  private final Mono<String> refreshedTokenMono = Mono.defer(this::refreshAccess)
      .cacheInvalidateIf(token -> tokenInvalid.get() || Duration.between(Instant.now(), token.getExpiresAt())
          .minusSeconds(30)
          .isNegative())
      .map(OAuth2AccessToken::getTokenValue);

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
    refreshTokenValue.set(loginPrefs.getRefreshToken());
    return refreshedTokenMono.then();
  }

  private void onTokenError(Throwable throwable) {
    log.warn("Could not retrieve token", throwable);
    loginPrefs.setRefreshToken(null);
    refreshTokenValue.set(null);
    tokenInvalid.set(true);
  }

  private Mono<OAuth2AccessToken> refreshAccess() {
    if (refreshTokenValue.get() == null) {
      loginPrefs.setRefreshToken(null);
      return Mono.error(new NoRefreshTokenException("No refresh token to log in with"));
    }

    Oauth oauth = clientProperties.getOauth();
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("refresh_token", refreshTokenValue.get());
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
          loginPrefs.setRefreshToken(loginPrefs.isRememberMe() ? refreshTokenValue.get() : null);
          tokenInvalid.set(false);
        })
        .map(OAuth2AccessTokenResponse::getAccessToken)
        .doOnNext(token -> log.info("Token valid until {}", token.getExpiresAt()));
  }

  public void invalidateToken() {
    tokenInvalid.set(true);
    refreshTokenValue.set(null);
    loginPrefs.setRefreshToken(null);
  }

  public boolean isTokenInvalid() {
    return tokenInvalid.get();
  }

  public ReadOnlyBooleanProperty tokenInvalidProperty() {
    return tokenInvalid.getReadOnlyProperty();
  }
}
