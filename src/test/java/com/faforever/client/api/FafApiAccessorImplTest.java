package com.faforever.client.api;

import com.faforever.client.fx.HostService;
import com.faforever.client.preferences.PreferencesService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FafApiAccessorImplTest {

  static class SpyableHttpTransport extends HttpTransport {

    LowLevelHttpRequest lowLevelHttpRequest;

    @Override
    public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
      return lowLevelHttpRequest;
    }
  }

  @Rule
  public TemporaryFolder preferencesDirectory = new TemporaryFolder();
  private FafApiAccessorImpl instance;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private LowLevelHttpRequest httpRequest;
  @Mock
  private ExecutorService executorService;
  @Mock
  private HostService hostServices;
  @Mock
  private VerificationCodeReceiver verificationCodeReceiver;
  @Mock
  private LowLevelHttpResponse lowLevelHttpResponse;
  @Spy
  private SpyableHttpTransport httpTransport;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    httpTransport.lowLevelHttpRequest = httpRequest;

    instance = new FafApiAccessorImpl();
    instance.preferencesService = preferencesService;
    instance.baseUrl = "http://api.example.com";
    instance.oAuthTokenServerUrl = "http://api.example.com/token";
    instance.oAuthClientSecret = "123";
    instance.oAuthClientId = "456";
    instance.oAuthAuthorizationServerUrl = "http://api.example.com/auth";
    instance.executorService = executorService;
    instance.hostServices = hostServices;
    instance.verificationCodeReceiver = verificationCodeReceiver;
    instance.httpTransport = httpTransport;
    instance.jsonFactory = new GsonFactory();

    when(preferencesService.getPreferencesDirectory()).thenReturn(preferencesDirectory.getRoot().toPath());

    when(lowLevelHttpResponse.getStatusCode()).thenReturn(200);
    when(httpRequest.execute()).thenReturn(lowLevelHttpResponse);

    instance.postConstruct();
  }

  @Test(expected = IllegalStateException.class)
  public void testGetPlayerAchievementsUnauthorizedThrowsIse() throws Exception {
    instance.getPlayerAchievements(123);
  }

  @Test
  public void testGetPlayerAchievements() throws Exception {
    instance.requestFactory = instance.httpTransport.createRequestFactory();
    instance.credential = mock(Credential.class);

    mockResponse("{'data': [" +
        " {" +
        "   'id': '1'," +
        "   'attributes': {'achievement_id': '1-2-3'}" +
        " }," +
        " {" +
        "   'id': '2'," +
        "   'attributes': {'achievement_id': '2-3-4'}" +
        " }" +
        "]}");

    PlayerAchievement playerAchievement1 = new PlayerAchievement();
    playerAchievement1.setId("1");
    playerAchievement1.setAchievementId("1-2-3");
    PlayerAchievement playerAchievement2 = new PlayerAchievement();
    playerAchievement2.setId("2");
    playerAchievement2.setAchievementId("2-3-4");
    List<PlayerAchievement> result = Arrays.asList(playerAchievement1, playerAchievement2);

    assertThat(instance.getPlayerAchievements(123), is(result));
    verify(httpTransport).buildRequest("GET", "http://api.example.com/players/123/achievements");
  }

  private void mockResponse(String string) throws IOException {
    when(lowLevelHttpResponse.getContent()).thenReturn(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void testGetAchievementDefinitions() throws Exception {
    instance.requestFactory = instance.httpTransport.createRequestFactory();
    instance.credential = mock(Credential.class);

    mockResponse("{'data': [" +
        " {" +
        "   'id': '1-2-3'," +
        "   'attributes': {}" +
        " }," +
        " {" +
        "   'id': '2-3-4'," +
        "   'attributes': {}" +
        " }" +
        "]}");

    AchievementDefinition achievementDefinition1 = new AchievementDefinition();
    achievementDefinition1.setId("1-2-3");
    AchievementDefinition achievementDefinition2 = new AchievementDefinition();
    achievementDefinition2.setId("2-3-4");
    List<AchievementDefinition> result = Arrays.asList(achievementDefinition1, achievementDefinition2);

    assertThat(instance.getAchievementDefinitions(), is(result));
    verify(httpTransport).buildRequest("GET", "http://api.example.com/achievements?sort=order");
  }

  @Test
  public void testGetAchievementDefinition() throws Exception {
    instance.requestFactory = instance.httpTransport.createRequestFactory();
    instance.credential = mock(Credential.class);

    AchievementDefinition achievementDefinition = new AchievementDefinition();
    achievementDefinition.setId("1-2-3");

    mockResponse("{'data': " +
        " {" +
        "   'id': '1-2-3'," +
        "   'attributes': {}" +
        " }" +
        "}");

    assertThat(instance.getAchievementDefinition("123"), is(achievementDefinition));
    verify(httpTransport).buildRequest("GET", "http://api.example.com/achievements/123");
  }

  @Test
  public void testAuthorize() throws Exception {
    when(verificationCodeReceiver.getRedirectUri()).thenReturn("http://localhost:1234");
    when(verificationCodeReceiver.waitForCode()).thenReturn("666");

    mockResponse("{}");

    instance.authorize(123);
    verify(hostServices).showDocument("http://api.example.com/auth?client_id=456&redirect_uri=http%3A%2F%2Flocalhost%3A1234&response_type=code&scope=read_achievements%20write_achievements%20write_events");
  }
}
