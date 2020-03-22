package com.faforever.client.api;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Api;
import com.google.gson.Gson;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;

@Service
@Slf4j
public class TokenService {
  private static final String OAUTH_TOKEN_PATH = "/oauth/token";

  private final ClientProperties clientProperties;
  private final RestTemplate simpleRestTemplate;
  private final ApplicationEventPublisher applicationEventPublisher;
  private OAuth2AccessToken tokenCache;

  public TokenService(ClientProperties clientProperties, ApplicationEventPublisher applicationEventPublisher) {
    this.clientProperties = clientProperties;
    this.applicationEventPublisher = applicationEventPublisher;

    simpleRestTemplate = new RestTemplateBuilder().
        build();
  }

  private Instant getExpireOfRefreshToke(String refreshToken) {
    String decode = JwtHelper.decode(refreshToken).getClaims();
    Gson gson = new Gson();
    RefreshToken refreshTokenObject = gson.fromJson(decode, RefreshToken.class);
    return Instant.ofEpochSecond(refreshTokenObject.getExp());
  }

  public OAuth2AccessToken getRefreshedToken() throws AuthenticationExpiredException {
    if (tokenCache != null && tokenCache.getRefreshToken() != null
        && getExpireOfRefreshToke(tokenCache.getRefreshToken().getValue()).isBefore(Instant.now())) {
      log.debug("Refresh Token expired");
      applicationEventPublisher.publishEvent(new SessionExpiredEvent());
      throw new AuthenticationExpiredException();
    }

    if (tokenCache == null || tokenCache.isExpired()) {
      log.debug("Token expired, fetching new token");
      refreshOAuthToken();
    } else {
      log.debug("Token still valid for {} seconds", tokenCache.getExpiresIn());
    }

    return tokenCache;
  }

  public void loginWithCredentials(String username, String password) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
    Api apiProperties = clientProperties.getApi();

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("grant_type", "password");
    map.add("client_secret", apiProperties.getClientSecret());
    map.add("client_id", apiProperties.getClientId());
    map.add("username", username);
    map.add("password", password);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
    try {

      tokenCache = simpleRestTemplate.postForObject(
          apiProperties.getBaseUrl() + OAUTH_TOKEN_PATH,
          request,
          OAuth2AccessToken.class
      );
    } catch (HttpClientErrorException e) {
      e.printStackTrace();
    }

  }

  private OAuth2AccessToken refreshOAuthToken() throws AuthenticationExpiredException {
    return loginWithRefreshToken(tokenCache.getRefreshToken().getValue());
  }

  public OAuth2AccessToken loginWithRefreshToken(String refreshToken) throws AuthenticationExpiredException {
    if (getExpireOfRefreshToke(refreshToken).isBefore(Instant.now())) {
      log.debug("Refresh Token expired");
      throw new AuthenticationExpiredException();
    }
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
    Api apiProperties = clientProperties.getApi();

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("grant_type", "refresh_token");
    map.add("client_secret", apiProperties.getClientSecret());
    map.add("client_id", apiProperties.getClientId());
    map.add("refresh_token", refreshToken);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

    try {
      tokenCache = simpleRestTemplate.postForObject(
          apiProperties.getBaseUrl() + OAUTH_TOKEN_PATH,
          request,
          OAuth2AccessToken.class
      );
    } catch (Exception e) {
      log.error("error", e);
    }

    return tokenCache;
  }

  @Data
  private static class RefreshToken {
    private long exp;
  }

  public static class AuthenticationExpiredException extends Exception {
    public AuthenticationExpiredException() {
      super("Session Expired");
    }
  }
}
