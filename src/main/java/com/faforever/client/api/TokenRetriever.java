package com.faforever.client.api;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.login.KnownLoginErrorException;
import com.faforever.client.login.NoRefreshTokenException;
import com.faforever.client.login.TokenRetrievalException;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.util.LogMaskingRegistry;
import com.faforever.client.util.LogMaskingRegistry.SensitiveValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jwt.JWTParser;
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
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

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

  private final Mono<OAuth2AccessToken> refreshedTokenMono = Mono.defer(this::refreshAccess)
                                                                 .cacheInvalidateWhen(this::getExpirationMono);

  @Override
  public void afterPropertiesSet() throws Exception {
    refreshTokenValue.addListener(
        (_, _, newValue) -> LogMaskingRegistry.update(SensitiveValueType.REFRESH_TOKEN, newValue));
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
    return refreshedTokenMono.map(OAuth2AccessToken::getTokenValue).doOnError(this::onTokenError);
  }

  public Mono<String> getRefreshedHmacValue() {
    return getRefreshedTokenValue().flatMap(tokenValue -> {
      try {
        return Mono.just(JWTParser.parse(tokenValue).getJWTClaimsSet().getJSONObjectClaim("ext").get("hmac").toString())
                   .doOnNext(value -> LogMaskingRegistry.update(SensitiveValueType.HMAC, value));
      } catch (Exception e) {
        return Mono.error(e);
      }
    });
  }

  /**
   * Starts the OAuth 2.0 device authorization flow (RFC 8628) by requesting a device and user code from the
   * authorization server.
   */
  public Mono<DeviceCodeResponse> initializeDeviceFlow() {
    Oauth oauth = clientProperties.getOauth();
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("client_id", oauth.getClientId());
    map.add("scope", oauth.getScopes());

    return defaultWebClient.post()
                           .uri(String.format("%s/oauth2/device/auth", oauth.getBaseUrl()))
                           .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                           .accept(MediaType.APPLICATION_JSON)
                           .bodyValue(map)
                           .exchangeToMono(response -> {
                             if (response.statusCode().isError()) {
                               return response.bodyToMono(String.class)
                                              .switchIfEmpty(Mono.just(response.statusCode().toString()))
                                              .flatMap(body -> Mono.error(new TokenRetrievalException(body)));
                             }

                             return response.bodyToMono(DeviceCodeResponse.class);
                           })
                           .doOnSubscribe(_ -> log.debug("Initializing device authorization flow"));
  }

  /**
   * Polls the token endpoint with the device code until the user approves (or denies) the request in the browser.
   * Honours the {@code interval} and {@code slow_down} pacing rules of RFC 8628 and gives up once the device code
   * expires.
   */
  public Mono<Void> loginWithDeviceCode(DeviceCodeResponse deviceCode) {
    Oauth oauth = clientProperties.getOauth();
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("device_code", deviceCode.deviceCode());
    map.add("client_id", oauth.getClientId());
    map.add("grant_type", "urn:ietf:params:oauth:grant-type:device_code");

    AtomicInteger intervalSeconds = new AtomicInteger(deviceCode.interval());

    return Mono.delay(Duration.ofSeconds(intervalSeconds.get()))
               .then(Mono.defer(() -> pollDeviceToken(map)))
               .retryWhen(Retry.from(signals -> signals.concatMap(signal -> {
                 Throwable failure = signal.failure();

                 return switch (failure) {
                   case SlowDownException _ -> {
                     int newInterval = intervalSeconds.addAndGet(5);
                     yield Mono.delay(Duration.ofSeconds(newInterval));
                   }
                   case AuthorizationPendingException _ ->
                       Mono.delay(Duration.ofSeconds(intervalSeconds.get()));
                   default -> Mono.<Long>error(failure);
                 };
               })))
               .timeout(Duration.ofSeconds(deviceCode.expiresIn()))
               .then();
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
    Mono<OAuth2AccessTokenResponse> responseMono =
        defaultWebClient.post()
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
                        .doOnSubscribe(_ -> log.debug("Retrieving OAuth token"));
    return handleTokenResponse(responseMono, false);
  }

  /**
   * A single poll against the token endpoint for the device code grant. Errors are translated into the RFC 8628
   * control-flow exceptions ({@link AuthorizationPendingException}, {@link SlowDownException}) so the caller can pace
   * its polling, or into terminal exceptions for {@code access_denied}/{@code expired_token}.
   */
  private Mono<OAuth2AccessToken> pollDeviceToken(MultiValueMap<String, String> properties) {
    Mono<OAuth2AccessTokenResponse> responseMono =
        defaultWebClient.post()
                        .uri(String.format("%s/oauth2/token", clientProperties.getOauth().getBaseUrl()))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(properties)
                        .exchangeToMono(response -> {
                          if (response.statusCode().isError()) {
                            return response.bodyToMono(OAuthError.class)
                                           .switchIfEmpty(Mono.just(new OAuthError(null, response.statusCode()
                                                                                                 .toString())))
                                           .flatMap(error -> Mono.error(mapDeviceError(error)));
                          }

                          return response.body(OAuth2BodyExtractors.oauth2AccessTokenResponse());
                        })
                        .doOnSubscribe(subscription -> log.debug("Polling for device authorization token"));
    return handleTokenResponse(responseMono, true);
  }

  private Mono<OAuth2AccessToken> handleTokenResponse(
      Mono<OAuth2AccessTokenResponse> responseMono,
      boolean clearRefreshTokenWhenMissing
  ) {
    return responseMono.doOnNext(tokenResponse -> {
                         OAuth2RefreshToken refreshToken = tokenResponse.getRefreshToken();
                         if (refreshToken != null || clearRefreshTokenWhenMissing) {
                           refreshTokenValue.set(refreshToken != null ? refreshToken.getTokenValue() : null);
                         }
                       })
                       .map(OAuth2AccessTokenResponse::getAccessToken)
                       .doOnNext(token -> {
                         LogMaskingRegistry.update(SensitiveValueType.ACCESS_TOKEN, token.getTokenValue());
                         log.info("Token valid until {}", token.getExpiresAt());
                       });
  }

  private Throwable mapDeviceError(OAuthError error) {
    String errorCode = error.error();

    return switch (errorCode) {
      case "authorization_pending" -> new AuthorizationPendingException();
      case "slow_down" -> new SlowDownException();
      case "access_denied" -> new KnownLoginErrorException("Device authorization was denied", "login.device.accessDenied");
      case "expired_token" -> new KnownLoginErrorException("Device code expired", "login.device.expired");
      case null, default ->
          new TokenRetrievalException(error.errorDescription() != null ? error.errorDescription() : errorCode);
    };
  }

  public void invalidateToken() {
    refreshTokenValue.set(null);
    invalidateSink.emitNext(0L, EmitFailureHandler.busyLooping(Duration.ofMillis(100)));
  }

  public Flux<Long> invalidationFlux() {
    return invalidateFlux;
  }

  private record OAuthError(@JsonProperty("error") String error,
                            @JsonProperty("error_description") String errorDescription) {}

  /** The device code grant has not been approved yet; the client should keep polling. */
  private static class AuthorizationPendingException extends RuntimeException {}

  /** The client is polling too fast and should increase its interval. */
  private static class SlowDownException extends RuntimeException {}
}
