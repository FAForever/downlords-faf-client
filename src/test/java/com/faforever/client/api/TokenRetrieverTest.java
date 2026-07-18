package com.faforever.client.api;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.login.KnownLoginErrorException;
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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TokenRetrieverTest extends ServiceTest {

  private static final String ACCESS_TOKEN = "access_token";
  private static final String REFRESH_TOKEN = "refresh_token";
  private static final String EXPIRES_IN = "expires_in";
  private static final String TOKEN_TYPE = "token_type";
  private static final DeviceCodeResponse DEVICE_CODE = new DeviceCodeResponse("device", "USER-CODE",
                                                                               "https://verify.faforever.com",
                                                                               "https://verify.faforever.com?user_code=USER-CODE",
                                                                               600, 1);
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

  private void prepareDeviceErrorResponse(String error) {
    mockApi.enqueue(new MockResponse().setResponseCode(400)
                                      .setBody(String.format("{\"error\":\"%s\"}", error))
                                      .addHeader("Content-Type", MediaType.APPLICATION_JSON));
  }

  private Map<String, String> takeRequestParams() throws Exception {
    String request = URLDecoder.decode(mockApi.takeRequest().getBody().readString(StandardCharsets.UTF_8),
                                       StandardCharsets.UTF_8);
    return Arrays.stream(request.split("&"))
                 .map(param -> param.split("="))
                 .map(keyValue -> Map.entry(keyValue[0], keyValue[1]))
                 .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  @Test
  public void testInitializeDeviceFlow() throws Exception {
    oauth.setScopes("openid offline");
    Map<String, Object> deviceProperties = Map.of("device_code", "device", "user_code", "USER-CODE",
                                                  "verification_uri", "https://verify.faforever.com",
                                                  "verification_uri_complete", "https://verify.faforever.com?user_code=USER-CODE",
                                                  "expires_in", 600, "interval", 5);
    mockApi.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(deviceProperties))
                                      .addHeader("Content-Type", MediaType.APPLICATION_JSON));

    StepVerifier.create(instance.initializeDeviceFlow()).assertNext(response -> {
      assertEquals("device", response.deviceCode());
      assertEquals("USER-CODE", response.userCode());
      assertEquals("https://verify.faforever.com", response.verificationUri());
      assertEquals("https://verify.faforever.com?user_code=USER-CODE", response.verificationUriComplete());
      assertEquals(600, response.expiresIn());
      assertEquals(5, response.intervalOrDefault());
    }).verifyComplete();

    Map<String, String> requestParams = takeRequestParams();
    assertEquals(oauth.getClientId(), requestParams.get("client_id"));
    assertEquals("openid offline", requestParams.get("scope"));
  }

  @Test
  public void testLoginWithDeviceCode() throws Exception {
    Map<String, String> tokenProperties = Map.of(ACCESS_TOKEN, "test", REFRESH_TOKEN, "refresh", EXPIRES_IN, "90", TOKEN_TYPE, "bearer");
    prepareTokenResponse(tokenProperties);

    StepVerifier.create(instance.loginWithDeviceCode(DEVICE_CODE)).verifyComplete();

    Map<String, String> requestParams = takeRequestParams();
    assertEquals(DEVICE_CODE.deviceCode(), requestParams.get("device_code"));
    assertEquals("urn:ietf:params:oauth:grant-type:device_code", requestParams.get("grant_type"));
    assertEquals(oauth.getClientId(), requestParams.get("client_id"));
  }

  @Test
  public void testLoginWithDeviceCodePolls() throws Exception {
    DeviceCodeResponse deviceCode = new DeviceCodeResponse("device", "USER-CODE", "https://verify.faforever.com",
                                                           "https://verify.faforever.com?user_code=USER-CODE", 600, 0);
    prepareDeviceErrorResponse("authorization_pending");
    prepareDeviceErrorResponse("slow_down");
    prepareTokenResponse(Map.of(ACCESS_TOKEN, "test", REFRESH_TOKEN, "refresh", EXPIRES_IN, "90", TOKEN_TYPE, "bearer"));

    StepVerifier.create(instance.loginWithDeviceCode(deviceCode))
                .expectComplete()
                .verify(Duration.ofSeconds(30));

    assertEquals(3, mockApi.getRequestCount());
  }

  @Test
  public void testLoginWithDeviceCodeDenied() {
    prepareDeviceErrorResponse("access_denied");

    StepVerifier.create(instance.loginWithDeviceCode(DEVICE_CODE)).verifyError(KnownLoginErrorException.class);
  }

  @Test
  public void testLoginWithDeviceCodeExpired() {
    prepareDeviceErrorResponse("expired_token");

    StepVerifier.create(instance.loginWithDeviceCode(DEVICE_CODE)).verifyError(KnownLoginErrorException.class);
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
  public void testTokenError() {
    prepareDeviceErrorResponse("invalid_request");
    StepVerifier.create(instance.loginWithDeviceCode(DEVICE_CODE)).verifyError(TokenRetrievalException.class);
  }

  @Test
  public void testGetRefreshToken() throws Exception {
    loginPrefs.setRememberMe(true);
    Map<String, String> tokenProperties = Map.of(EXPIRES_IN, "3600", REFRESH_TOKEN, "refresh", ACCESS_TOKEN, "test", TOKEN_TYPE, "bearer");
    prepareTokenResponse(tokenProperties);

    StepVerifier.create(instance.loginWithDeviceCode(DEVICE_CODE)).verifyComplete();

    assertEquals(tokenProperties.get(REFRESH_TOKEN), loginPrefs.getRefreshToken());
  }

  @Test
  public void testGetAccessToken() throws Exception {
    loginPrefs.setRememberMe(true);
    Map<String, String> tokenProperties = Map.of(EXPIRES_IN, "3600", REFRESH_TOKEN, "refresh", ACCESS_TOKEN, "test",
                                                 TOKEN_TYPE, "bearer");
    prepareTokenResponse(tokenProperties);

    StepVerifier.create(instance.getRefreshedTokenValue())
                .assertNext(accessToken -> assertEquals("test", accessToken))
                .verifyComplete();
  }
}
