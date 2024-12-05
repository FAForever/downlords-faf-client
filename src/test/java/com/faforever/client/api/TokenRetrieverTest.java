package com.faforever.client.api;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.login.NoRefreshTokenException;
import com.faforever.client.login.TokenRetrievalException;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.test.ServiceTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TokenRetrieverTest extends ServiceTest {

  private static final String VERIFIER = "def";
  private static final URI REDIRECT_URI = URI.create("http://localhost:123");
  private static final String ACCESS_TOKEN = "access_token";
  private static final String REFRESH_TOKEN = "refresh_token";
  private static final String EXPIRES_IN = "expires_in";
  private static final String TOKEN_TYPE = "token_type";
  private TokenRetriever instance;

  private LoginPrefs loginPrefs;
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
    loginPrefs = new LoginPrefs();
    loginPrefs.setRefreshToken("abc");

    instance = new TokenRetriever(clientProperties, WebClient.builder().build(), loginPrefs);
    instance.afterPropertiesSet();
  }

  private void prepareTokenResponse(Map<String, String> tokenProperties) throws Exception {
    mockApi.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(tokenProperties))
        .addHeader("Content-Type", MediaType.APPLICATION_JSON));
  }

  private void prepareErrorResponse() {
    mockApi.enqueue(new MockResponse().setResponseCode(400).addHeader("Content-Type", MediaType.APPLICATION_JSON));
  }

  @Test
  public void testLoginWithCode() throws Exception {
    Map<String, String> tokenProperties = Map.of(ACCESS_TOKEN, "test", REFRESH_TOKEN, "refresh", EXPIRES_IN, "90", TOKEN_TYPE, "bearer");
    prepareTokenResponse(tokenProperties);
    StepVerifier verifier = StepVerifier.create(instance.invalidationFlux())
                                        .expectNextCount(0)
                                        .thenCancel()
                                        .verifyLater();

    StepVerifier.create(instance.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI)).verifyComplete();
    String request = URLDecoder.decode(mockApi.takeRequest()
        .getBody()
        .readString(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

    Map<String, String> requestParams = Arrays.stream(request.split("&"))
        .map(param -> param.split("="))
        .map(keyValue -> Map.entry(keyValue[0], keyValue[1]))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    assertEquals("abc", requestParams.get("code"));
    assertEquals("authorization_code", requestParams.get("grant_type"));
    assertEquals(oauth.getClientId(), requestParams.get("client_id"));
    assertEquals(REDIRECT_URI.toString(), requestParams.get("redirect_uri"));
    verifier.verify(Duration.ofSeconds(1));
  }

  @Test
  public void testLoginWithRefresh() throws Exception {
    Map<String, String> tokenProperties = Map.of(ACCESS_TOKEN, "test", REFRESH_TOKEN, "refresh", EXPIRES_IN, "90", TOKEN_TYPE, "bearer");
    prepareTokenResponse(tokenProperties);

    StepVerifier verifier = StepVerifier.create(instance.invalidationFlux())
                                        .expectNextCount(0)
                                        .thenCancel()
                                        .verifyLater();

    StepVerifier.create(instance.loginWithRefreshToken()).verifyComplete();
    String request = URLDecoder.decode(mockApi.takeRequest()
        .getBody()
        .readString(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

    Map<String, String> requestParams = Arrays.stream(request.split("&"))
        .map(param -> param.split("="))
        .map(keyValue -> Map.entry(keyValue[0], keyValue[1]))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    assertEquals(REFRESH_TOKEN, requestParams.get("grant_type"));
    assertEquals(oauth.getClientId(), requestParams.get("client_id"));
    verifier.verify(Duration.ofSeconds(1));
  }

  @Test
  public void testGetRefreshedTokenExpired() throws Exception {
    prepareTokenResponse(Map.of(EXPIRES_IN, "0", REFRESH_TOKEN, "refresh", ACCESS_TOKEN, "test", TOKEN_TYPE, "bearer"));

    StepVerifier.create(instance.loginWithRefreshToken()).verifyComplete();

    Thread.sleep(100);

    Map<String, String> tokenProperties = Map.of(EXPIRES_IN, "100", REFRESH_TOKEN, "refresh", ACCESS_TOKEN, "new", TOKEN_TYPE, "bearer");
    prepareTokenResponse(tokenProperties);

    StepVerifier.create(instance.getRefreshedTokenValue())
        .expectNext(tokenProperties.get(ACCESS_TOKEN))
        .verifyComplete();
  }

  @Test
  public void testGetRefreshedTokenError() throws Exception {
    prepareTokenResponse(Map.of(EXPIRES_IN, "-1", REFRESH_TOKEN, "refresh", ACCESS_TOKEN, "test", TOKEN_TYPE, "bearer"));

    StepVerifier.create(instance.loginWithRefreshToken()).verifyComplete();

    Thread.sleep(100);

    prepareErrorResponse();

    StepVerifier.create(instance.getRefreshedTokenValue()).verifyError(TokenRetrievalException.class);
  }

  @Test
  public void testInvalidation() throws Exception {
    prepareTokenResponse(Map.of(EXPIRES_IN, "3600", REFRESH_TOKEN, "refresh", ACCESS_TOKEN, "test", TOKEN_TYPE, "bearer"));

    StepVerifier.create(instance.loginWithRefreshToken()).verifyComplete();

    Thread.sleep(100);

    instance.invalidateToken();

    prepareErrorResponse();

    StepVerifier.create(instance.getRefreshedTokenValue()).verifyError(TokenRetrievalException.class);
  }

  @Test
  public void testNoToken() {
    instance.invalidateToken();
    StepVerifier.create(instance.getRefreshedTokenValue()).verifyError(NoRefreshTokenException.class);
  }

  @Test
  public void testTokenError() throws Exception {
    prepareErrorResponse();
    StepVerifier.create(instance.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI))
        .verifyError(TokenRetrievalException.class);
  }

  @Test
  public void testGetRefreshToken() throws Exception {
    loginPrefs.setRememberMe(true);
    Map<String, String> tokenProperties = Map.of(EXPIRES_IN, "3600", REFRESH_TOKEN, "refresh", ACCESS_TOKEN, "test", TOKEN_TYPE, "bearer");
    prepareTokenResponse(tokenProperties);

    StepVerifier.create(instance.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI)).verifyComplete();

    assertEquals(tokenProperties.get(REFRESH_TOKEN), loginPrefs.getRefreshToken());
  }

  @Test
  public void testGetAccessToken() throws Exception {
    loginPrefs.setRememberMe(true);
    Map<String, String> tokenProperties = Map.of(EXPIRES_IN, "3600", REFRESH_TOKEN, "refresh", ACCESS_TOKEN, "test",
                                                 TOKEN_TYPE, "bearer");
    prepareTokenResponse(tokenProperties);

    StepVerifier.create(instance.getAccessToken())
                .assertNext(accessToken -> assertEquals(accessToken, "test"))
                .verifyComplete();
  }
}
