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
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

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
  private MockWebServer mockApi;

  private static final String TOKEN_STRING = """
    {
    "access_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6InB1YmxpYzo0NmFiZmM4OC04Y2YwLTRkMzUtYTc2Zi01MzhlMTMwMTZiZmQiLCJ0eXAiOiJKV1QifQ.eyJhdWQiOltdLCJjbGllbnRfaWQiOiJmYWYtamF2YS1jbGllbnQiLCJleHAiOjE2MzQyMjA3NjgsImV4dCI6eyJyb2xlcyI6WyJVU0VSIiwiUkVBRF9BVURJVF9MT0ciLCJXUklURV9BVkFUQVIiLCJBRE1JTl9WT1RFIiwiV1JJVEVfVVNFUl9HUk9VUCIsIlJFQURfVVNFUl9HUk9VUCIsIldSSVRFX05FV1NfUE9TVCIsIldSSVRFX01FU1NBR0UiLCJXUklURV9NQVRDSE1BS0VSX1BPT0wiLCJXUklURV9NQVRDSE1BS0VSX01BUCIsIldSSVRFX0VNQUlMX0RPTUFJTl9CQU4iLCJBRE1JTl9NQVAiLCJBRE1JTl9NT0QiXX0sImlhdCI6MTYzNDIxNzE2NywiaXNzIjoiaHR0cHM6Ly9oeWRyYS50ZXN0LmZhZm9yZXZlci5jb20vIiwianRpIjoiYWMxYzEyZDItYWM3Yi00ZTVlLWJmMjgtZTFkODNkOTc2MDY0IiwibmJmIjoxNjM0MjE3MTY3LCJzY3AiOlsib2ZmbGluZSJdLCJzdWIiOiI2NTM0MSJ9.ilQvPmb7Itd9AVqe8A4mUciWVHwoSGXnU5zsw1Qvx3swkktl-ly200gECGYOsBeh7DxvsKIqUUHPuokSZBpxfE8fwb4c7yO6t-4XbZzS5GtE4q0Vuu-3q1UXfRZZqSYFG8CispEqqheQyzw7nBjMa8JYIB5LrmwPcoa-ilEzzuiZM-MXI7Dkx_K5x5znpvQUTn8u9nKdg1C4E3_0fQHRIjZ6zzGLcMMY4j6PUSKrosJ8rQAOqyMRaH2gELB8tCZCrFr8BbXJc2Fzcp6Y0cj4eyYe2CnZDfIjN3bjqXNzADXpgrmlYPegphurUMSyoqC58eRwdd8FJtl33aNcSdhg-gLY3bYvuBhtMWE3C9P9ahPLUceJkoCU3bmba615bjd47e0j_6i9AskAOwXsNIojvOk0UcCPHzIYMG8prJJ6MVtRoujrGWZROuPs6DuXpvUOppeCf2nrEsCsOsrAK60wYA8u7S1ey-gPpFCyll7i472ENXo2WBEen6r-UZHs_WOUvS1oKCpAKcaPHpStoq2rUbtagSGsmBelhYSS5s7uak8MVqm1EQsuwL6TvgfNOk3F6PD8_2955-IcTKuHWd5RkZiPbGM-ZclAHZCoAOcuTchuMRGy72pNJ9y8iqNZJct7SwLuJ81BNRL0ByxbmN4YUyWgCy1DPJ3ubm2T82xGTlM",
    "expires_in": 3600,
    "refresh_token": "mmAvoed0ZGeJu_wGpaXHBicRUNyp884gMZpkljY1ax0.fpHiq5zJ1MZQvAenO4BudU_cQ0dRy_D1QPOw_jB6rj0",
    "scope": "offline",
    "token_type": "bearer"
  }
  """;

  @BeforeEach
  public void setUp() throws Exception {
    ClientProperties clientProperties = new ClientProperties();
    Oauth oauth = new Oauth();
    oauth.setBaseUrl("test.com");
    oauth.setClientId("");
    oauth.setRedirectUrl("");
    clientProperties.setOauth(oauth);
    preferences = PreferencesBuilder.create().defaultValues().get();
    mockApi = new MockWebServer();
    mockApi.start();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(restTemplateBuilder.build()).thenReturn(restTemplate);

    instance = new TokenService(clientProperties, preferencesService, eventBus, WebClient.builder());
    instance.afterPropertiesSet();

    verify(eventBus).register(instance);
  }

  private void prepareTokenResponse() {
    mockApi.
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
