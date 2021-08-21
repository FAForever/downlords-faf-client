package com.faforever.client.api;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.login.TokenRetrievalException;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TokenServiceTest extends ServiceTest {
  private TokenService instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private EventBus eventBus;
  @Mock
  private RestTemplateBuilder restTemplateBuilder;
  @Mock
  private RestTemplate restTemplate;
  @Mock
  private OAuth2AccessToken testToken;

  private Preferences preferences;

  @BeforeEach
  public void setUp() throws Exception {
    ClientProperties clientProperties = new ClientProperties();
    Oauth oauth = new Oauth();
    oauth.setBaseUrl("test.com");
    oauth.setClientId("");
    oauth.setRedirectUrl("");
    clientProperties.setOauth(oauth);
    preferences = PreferencesBuilder.create().defaultValues().get();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(restTemplateBuilder.build()).thenReturn(restTemplate);

    instance = new TokenService(clientProperties, preferencesService, eventBus, restTemplateBuilder);
    instance.afterPropertiesSet();

    verify(eventBus).register(instance);
  }

  @Test
  public void testLoginWithCode() {
    when(restTemplate.postForObject(anyString(), any(), eq(OAuth2AccessToken.class))).thenReturn(testToken);
    when(testToken.isExpired()).thenReturn(false);
    when(testToken.getExpiresIn()).thenReturn(10);
    when(testToken.getValue()).thenReturn("def");

    instance.loginWithAuthorizationCode("abc");

    assertEquals(testToken.getValue(), instance.getRefreshedTokenValue());
  }

  @Test
  public void testLoginWithRefresh() {
    when(restTemplate.postForObject(anyString(), any(), eq(OAuth2AccessToken.class))).thenReturn(testToken);
    when(testToken.isExpired()).thenReturn(false);
    when(testToken.getExpiresIn()).thenReturn(10);
    when(testToken.getValue()).thenReturn("def");

    instance.loginWithRefreshToken("abc");

    assertEquals(testToken.getValue(), instance.getRefreshedTokenValue());
  }

  @Test
  public void testGetRefreshedTokenExpired() {
    when(restTemplate.postForObject(anyString(), any(), eq(OAuth2AccessToken.class))).thenReturn(testToken);
    when(testToken.isExpired()).thenReturn(true);
    OAuth2RefreshToken refreshToken = mock(OAuth2RefreshToken.class);
    when(testToken.getRefreshToken()).thenReturn(refreshToken);
    when(refreshToken.getValue()).thenReturn("qwe");

    instance.loginWithRefreshToken("abc");

    OAuth2AccessToken newToken = mock(OAuth2AccessToken.class);
    when(restTemplate.postForObject(anyString(), any(), eq(OAuth2AccessToken.class))).thenReturn(newToken);
    when(newToken.getValue()).thenReturn("fgr");

    assertEquals(newToken.getValue(), instance.getRefreshedTokenValue());
  }

  @Test
  public void testGetRefreshedTokenError() {
    when(restTemplate.postForObject(anyString(), any(), eq(OAuth2AccessToken.class))).thenReturn(testToken);
    when(testToken.isExpired()).thenReturn(true);
    OAuth2RefreshToken refreshToken = mock(OAuth2RefreshToken.class);
    when(testToken.getRefreshToken()).thenReturn(refreshToken);
    when(refreshToken.getValue()).thenReturn("qwe");

    instance.loginWithRefreshToken("abc");

    doThrow(new FakeTestException()).when(restTemplate).postForObject(anyString(), any(), eq(OAuth2AccessToken.class));

    assertNull(instance.getRefreshedTokenValue());
    verify(eventBus).post(any(SessionExpiredEvent.class));
  }

  @Test
  public void testNoToken() {
    assertNull(instance.getRefreshedTokenValue());
    verify(eventBus).post(any(LogOutRequestEvent.class));
  }

  @Test
  public void testTokenIsNull() {
    when(restTemplate.postForObject(anyString(), any(), eq(OAuth2AccessToken.class))).thenReturn(null);

    assertThrows(TokenRetrievalException.class, () -> instance.loginWithAuthorizationCode("abc"));
  }

  @Test
  public void testGetRefreshToken() {
    when(restTemplate.postForObject(anyString(), any(), eq(OAuth2AccessToken.class))).thenReturn(testToken);
    preferences.getLogin().setRememberMe(true);
    OAuth2RefreshToken refreshToken = mock(OAuth2RefreshToken.class);
    when(refreshToken.getValue()).thenReturn("qwe");
    when(testToken.getRefreshToken()).thenReturn(refreshToken);

    instance.loginWithAuthorizationCode("abc");

    assertEquals("qwe", instance.getRefreshToken());
    assertEquals("qwe", preferences.getLogin().getRefreshToken());
  }

  @Test
  public void testGetRefreshTokenNull() {
    when(restTemplate.postForObject(anyString(), any(), eq(OAuth2AccessToken.class))).thenReturn(testToken);
    preferences.getLogin().setRememberMe(false);
    OAuth2RefreshToken refreshToken = mock(OAuth2RefreshToken.class);
    when(testToken.getRefreshToken()).thenReturn(refreshToken);

    instance.loginWithAuthorizationCode("abc");

    assertNull(instance.getRefreshToken());
    assertNull(preferences.getLogin().getRefreshToken());
  }
}
