package com.faforever.client.api;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.login.NoRefreshTokenException;
import com.faforever.client.login.TokenRetrievalException;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.event.LoggedOutEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TokenServiceTest extends ServiceTest {
  private TokenService instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private EventBus eventBus;

  private Preferences preferences;
  private Oauth oauth;
  private MockWebServer mockApi;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  public void setUp() throws Exception {
    ClientProperties clientProperties = new ClientProperties();
    mockApi = new MockWebServer();
    mockApi.start();
    oauth = clientProperties.getOauth();
    oauth.setBaseUrl(String.format("http://localhost:%s", mockApi.getPort()));
    oauth.setClientId("test-client");
    oauth.setRedirectUrl("test-redirect");
    preferences = PreferencesBuilder.create().defaultValues().loginPrefs().refreshToken("abc").then().get();

    when(preferencesService.getPreferences()).thenReturn(preferences);

    instance = new TokenService(clientProperties, preferencesService, eventBus, WebClient.builder());
    instance.afterPropertiesSet();

    verify(eventBus).register(instance);
  }

  private void prepareTokenResponse(Map<String, String> tokenProperties) throws Exception{
    mockApi.enqueue(new MockResponse()
        .setBody(objectMapper.writeValueAsString(tokenProperties))
        .addHeader("Content-Type", MediaType.APPLICATION_JSON));
  }

  @Test
  public void testLoginWithCode() throws Exception {
    Map<String, String> tokenProperties = Map.of(OAuth2AccessToken.ACCESS_TOKEN, "test", OAuth2AccessToken.REFRESH_TOKEN, "refresh", OAuth2AccessToken.EXPIRES_IN, "90");
    prepareTokenResponse(tokenProperties);

    StepVerifier.create(instance.loginWithAuthorizationCode("abc"))
        .verifyComplete();
    Map<String, String> requestParams = URLEncodedUtils.parse(mockApi.takeRequest().getBody().readString(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
        .stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));

    assertEquals("abc", requestParams.get("code"));
    assertEquals("authorization_code", requestParams.get("grant_type"));
    assertEquals(oauth.getClientId(), requestParams.get("client_id"));
    assertEquals(oauth.getRedirectUrl(), requestParams.get("redirect_uri"));

    Thread.sleep(10);

    StepVerifier.create(instance.getRefreshedTokenValue())
        .expectNext(tokenProperties.get(OAuth2AccessToken.ACCESS_TOKEN))
        .verifyComplete();
  }

  @Test
  public void testLoginWithRefresh() throws Exception {
    Map<String, String> tokenProperties = Map.of(OAuth2AccessToken.ACCESS_TOKEN, "test", OAuth2AccessToken.REFRESH_TOKEN, "refresh", OAuth2AccessToken.EXPIRES_IN, "90");
    prepareTokenResponse(tokenProperties);

    StepVerifier.create(instance.loginWithRefreshToken())
        .verifyComplete();
    Map<String, String> requestParams = URLEncodedUtils.parse(mockApi.takeRequest().getBody().readString(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
        .stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));

    assertEquals("abc", requestParams.get("refresh_token"));
    assertEquals("refresh_token", requestParams.get("grant_type"));
    assertEquals(oauth.getClientId(), requestParams.get("client_id"));
    assertEquals(oauth.getRedirectUrl(), requestParams.get("redirect_uri"));

    Thread.sleep(10);

    StepVerifier.create(instance.getRefreshedTokenValue())
            .expectNext(tokenProperties.get(OAuth2AccessToken.ACCESS_TOKEN))
                .verifyComplete();
  }

  @Test
  public void testGetRefreshedTokenExpired() throws Exception {
    prepareTokenResponse(Map.of(OAuth2AccessToken.EXPIRES_IN, "-1", OAuth2AccessToken.REFRESH_TOKEN, "refresh", OAuth2AccessToken.ACCESS_TOKEN, "test"));

    StepVerifier.create(instance.loginWithRefreshToken())
        .verifyComplete();

    Thread.sleep(10);

    Map<String, String> tokenProperties = Map.of(OAuth2AccessToken.ACCESS_TOKEN, "new");
    prepareTokenResponse(tokenProperties);

    StepVerifier.create(instance.getRefreshedTokenValue())
        .expectNext(tokenProperties.get(OAuth2AccessToken.ACCESS_TOKEN))
        .verifyComplete();
  }

  @Test
  public void testGetRefreshedTokenError() throws Exception {
    prepareTokenResponse(Map.of(OAuth2AccessToken.EXPIRES_IN, "-1", OAuth2AccessToken.REFRESH_TOKEN, "refresh", OAuth2AccessToken.ACCESS_TOKEN, "test"));

    StepVerifier.create(instance.loginWithRefreshToken())
        .verifyComplete();

    Thread.sleep(10);

    prepareTokenResponse(null);

    StepVerifier.create(instance.getRefreshedTokenValue())
        .verifyError(TokenRetrievalException.class);

    verify(eventBus).post(any(SessionExpiredEvent.class));
  }

  @Test
  public void testLogOutInvalidates() throws Exception {
    prepareTokenResponse(Map.of(OAuth2AccessToken.EXPIRES_IN, "3600", OAuth2AccessToken.REFRESH_TOKEN, "refresh", OAuth2AccessToken.ACCESS_TOKEN, "test"));

    StepVerifier.create(instance.loginWithRefreshToken())
        .verifyComplete();

    Thread.sleep(10);

    instance.onLogOut(new LoggedOutEvent());

    prepareTokenResponse(null);

    StepVerifier.create(instance.getRefreshedTokenValue())
        .verifyError(TokenRetrievalException.class);

    verify(eventBus).post(any(SessionExpiredEvent.class));
  }

  @Test
  public void testNoToken() {
    StepVerifier.create(instance.getRefreshedTokenValue())
        .verifyError(NoRefreshTokenException.class);

    verify(eventBus).post(any(SessionExpiredEvent.class));
  }

  @Test
  public void testTokenIsNull() throws Exception {
    prepareTokenResponse(null);
    StepVerifier.create(instance.loginWithAuthorizationCode("abc"))
            .verifyError(TokenRetrievalException.class);
  }

  @Test
  public void testGetRefreshToken() throws Exception {
    preferences.getLogin().setRememberMe(true);
    Map<String, String> tokenProperties = Map.of(OAuth2AccessToken.REFRESH_TOKEN, "refresh");
    prepareTokenResponse(tokenProperties);

    StepVerifier.create(instance.loginWithAuthorizationCode("abc"))
        .verifyComplete();

    assertEquals(tokenProperties.get(OAuth2AccessToken.REFRESH_TOKEN), preferences.getLogin().getRefreshToken());
  }

  @Test
  public void testGetRefreshTokenNull() throws Exception {
    preferences.getLogin().setRememberMe(false);
    prepareTokenResponse(Map.of(OAuth2AccessToken.REFRESH_TOKEN, "refresh"));

    instance.loginWithAuthorizationCode("abc").block();

    assertNull(preferences.getLogin().getRefreshToken());
  }
}
