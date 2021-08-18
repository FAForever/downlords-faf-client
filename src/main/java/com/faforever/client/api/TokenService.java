package com.faforever.client.api;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.login.TokenRetrievalException;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.google.common.eventbus.EventBus;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Service
@Slf4j
public class TokenService implements InitializingBean {
  private final ClientProperties clientProperties;
  private final PreferencesService preferencesService;
  private final EventBus eventBus;
  private final RestTemplate restTemplate;
  private OAuth2AccessToken tokenCache;

  public TokenService(ClientProperties clientProperties, PreferencesService preferencesService, EventBus eventBus, RestTemplateBuilder restTemplateBuilder) {
    this.clientProperties = clientProperties;
    this.preferencesService = preferencesService;
    this.eventBus = eventBus;

    restTemplate = restTemplateBuilder.build();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    eventBus.register(this);
  }

  @SneakyThrows
  public synchronized String getRefreshedTokenValue() {
    if (tokenCache != null) {
      try {
        if (tokenCache.isExpired()) {
          log.debug("Token expired, fetching new token");
          loginWithRefreshToken(tokenCache.getRefreshToken().getValue());
        } else {
          log.debug("Token still valid for {} seconds", tokenCache.getExpiresIn());
        }

        return tokenCache.getValue();
      } catch (Exception e) {
        log.info("Could not login with token", e);
        tokenCache = null;
        preferencesService.getPreferences().getLogin().setRefreshToken(getRefreshToken());
        preferencesService.storeInBackground();
        eventBus.post(new SessionExpiredEvent());
        return null;
      }
    } else {
      log.warn("No valid token found to be refreshed");
      eventBus.post(new LogOutRequestEvent());
      return null;
    }
  }

  public synchronized void loginWithAuthorizationCode(String code) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("code", code);
    Oauth oauth = clientProperties.getOauth();
    map.add("client_id", oauth.getClientId());
    map.add("redirect_uri", oauth.getRedirectUrl());
    map.add("grant_type", "authorization_code");

    retrieveToken(headers, map, oauth);
  }

  public synchronized void loginWithRefreshToken(String refreshToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));

    Oauth oauth = clientProperties.getOauth();
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("refresh_token", refreshToken);
    map.add("client_id", oauth.getClientId());
    map.add("redirect_uri", oauth.getRedirectUrl());
    map.add("grant_type", "refresh_token");

    retrieveToken(headers, map, oauth);
  }

  private void retrieveToken(HttpHeaders headers, MultiValueMap<String, String> map, Oauth oauth) {
    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
    log.debug("Retrieving OAuth token");
    tokenCache = restTemplate.postForObject(
        String.format("%s/oauth2/token", oauth.getBaseUrl()),
        request,
        OAuth2AccessToken.class
    );

    preferencesService.getPreferences().getLogin().setRefreshToken(getRefreshToken());
    preferencesService.storeInBackground();

    if (tokenCache == null) {
      throw new TokenRetrievalException("Could not login with provided parameters");
    }

    preferencesService.getPreferences().getLogin().setRefreshToken(getRefreshToken());
    preferencesService.storeInBackground();
  }

  public String getRefreshToken() {
    if (tokenCache != null
        && tokenCache.getRefreshToken() != null
        && preferencesService.getPreferences().getLogin().getRememberMe()) {
      return tokenCache.getRefreshToken().getValue();
    } else {
      return null;
    }
  }
}
