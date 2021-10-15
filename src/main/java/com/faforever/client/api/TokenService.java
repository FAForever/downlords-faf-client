package com.faforever.client.api;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.login.TokenRetrievalException;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class TokenService implements InitializingBean {
  private final ClientProperties clientProperties;
  private final PreferencesService preferencesService;
  private final EventBus eventBus;
  private final WebClient webClient;
  private OAuth2AccessToken tokenCache;

  public TokenService(ClientProperties clientProperties, PreferencesService preferencesService, EventBus eventBus, WebClient.Builder webClientBuilder) {
    this.clientProperties = clientProperties;
    this.preferencesService = preferencesService;
    this.eventBus = eventBus;

    webClient = webClientBuilder.build();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    eventBus.register(this);
  }

  public synchronized String getRefreshedTokenValue() {
    if (tokenCache == null) {
      log.warn("No valid token found to be refreshed");
      eventBus.post(new LogOutRequestEvent());
      return null;
    }

    try {
      if (tokenCache.isExpired()) {
        log.debug("Token expired, fetching new token");
        loginWithRefreshToken(tokenCache.getRefreshToken().getValue());
      }

      log.debug("Token still valid for {} seconds", tokenCache.getExpiresIn());
      return tokenCache.getValue();
    } catch (Exception e) {
      log.info("Could not login with token", e);
      tokenCache = null;
      preferencesService.getPreferences().getLogin().setRefreshToken(getRefreshToken());
      preferencesService.storeInBackground();
      eventBus.post(new SessionExpiredEvent());
      return null;
    }
  }

  public synchronized void loginWithAuthorizationCode(String code) {
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    Oauth oauth = clientProperties.getOauth();
    map.add("code", code);
    map.add("client_id", oauth.getClientId());
    map.add("redirect_uri", oauth.getRedirectUrl());
    map.add("grant_type", "authorization_code");

    retrieveToken(map);
  }

  public synchronized void loginWithRefreshToken(String refreshToken) {
    Oauth oauth = clientProperties.getOauth();
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("refresh_token", refreshToken);
    map.add("client_id", oauth.getClientId());
    map.add("redirect_uri", oauth.getRedirectUrl());
    map.add("grant_type", "refresh_token");

    retrieveToken(map);
  }

  private void retrieveToken(MultiValueMap<String, String> map) {
    log.debug("Retrieving OAuth token");
    tokenCache = webClient.post()
        .uri(String.format("%s/oauth2/token", clientProperties.getOauth().getBaseUrl()))
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(map)
        .retrieve()
        .bodyToMono(OAuth2AccessToken.class)
        .block();

    preferencesService.getPreferences().getLogin().setRefreshToken(getRefreshToken());
    preferencesService.storeInBackground();

    if (tokenCache == null) {
      throw new TokenRetrievalException("Could not login with provided parameters");
    }

    preferencesService.getPreferences().getLogin().setRefreshToken(getRefreshToken());
    preferencesService.storeInBackground();
  }

  public String getRefreshToken() {
    if (tokenCache == null
        || tokenCache.getRefreshToken() == null
        || !preferencesService.getPreferences().getLogin().getRememberMe()) {
      return null;
    }

    return tokenCache.getRefreshToken().getValue();
  }
}
