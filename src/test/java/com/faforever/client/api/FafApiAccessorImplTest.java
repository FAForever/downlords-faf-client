package com.faforever.client.api;

import com.faforever.client.fx.HostService;
import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.mod.ModInfoBean;
import com.faforever.client.mod.ModInfoBeanBuilder;
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
import org.mockito.stubbing.OngoingStubbing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
    instance.oAuthUrl = "http://api.example.com/auth";
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

  private void mockResponse(String... responses) throws IOException {
    OngoingStubbing<InputStream> ongoingStubbing = when(lowLevelHttpResponse.getContent());
    for (String string : responses) {
      ongoingStubbing = ongoingStubbing.thenReturn(new ByteArrayInputStream(string.getBytes(UTF_8)));
    }
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
    verify(hostServices).showDocument("http://api.example.com/auth?client_id=456&redirect_uri=http%3A%2F%2Flocalhost%3A1234&response_type=code&scope=read_achievements%20read_events");
  }

  @Test
  public void testGetPlayerEvents() throws Exception {
    instance.requestFactory = instance.httpTransport.createRequestFactory();
    instance.credential = mock(Credential.class);

    mockResponse("{'data': [" +
        " {" +
        "   'id': '1'," +
        "   'attributes': {'count': 11, 'event_id': '1-1-1' }" +
        " }," +
        " {" +
        "   'id': '2'," +
        "   'attributes': {'count': 22, 'event_id': '2-2-2' }" +
        " }" +
        "]}");

    PlayerEvent playerEvent1 = new PlayerEvent();
    playerEvent1.setId("1");
    playerEvent1.setEventId("1-1-1");
    playerEvent1.setCount(11);
    PlayerEvent playerEvent2 = new PlayerEvent();
    playerEvent2.setId("2");
    playerEvent2.setEventId("2-2-2");
    playerEvent2.setCount(22);

    List<PlayerEvent> result = Arrays.asList(playerEvent1, playerEvent2);

    assertThat(instance.getPlayerEvents(123), is(result));
    verify(httpTransport).buildRequest("GET", "http://api.example.com/players/123/events");
  }

  @Test
  public void testGetMods() throws Exception {
    instance.requestFactory = instance.httpTransport.createRequestFactory();
    instance.credential = mock(Credential.class);

    mockResponse("{'data': [" +
            " {" +
            "   'id': '1'," +
            "   'attributes': {" +
            "     'create_time': '2011-12-03T10:15:30'," +
            "     'download_url': 'http://example.com/mod1.zip'" +
            "   }" +
            " }," +
            " {" +
            "   'id': '2'," +
            "   'attributes': {" +
            "     'create_time': '2011-12-03T10:15:30'," +
            "     'download_url': 'http://example.com/mod2.zip'" +
            "   }" +
            " }" +
            "]}",
        "{'data': []}");

    List<ModInfoBean> result = Arrays.asList(
        ModInfoBeanBuilder.create().defaultValues().uid("1").get(),
        ModInfoBeanBuilder.create().defaultValues().uid("2").get()
    );

    assertThat(instance.getMods(), equalTo(result));
    verify(httpTransport, times(2)).buildRequest("GET", "http://api.example.com/mods");
  }

  @Test
  public void testGetRanked1v1Entries() throws Exception {
    instance.requestFactory = instance.httpTransport.createRequestFactory();
    instance.credential = mock(Credential.class);


    mockResponse("{'data': [" +
            " {" +
            "   'id': '1'," +
            "   'attributes': {" +
            "     'login': 'user1'," +
            "     'num_games': 5" +
            "   }" +
            " }," +
            " {" +
            "   'id': '2'," +
            "   'attributes': {" +
            "     'login': 'user2'," +
            "     'num_games': 3" +
            "   }" +
            " }" +
            "]}",
        "{'data': []}");

    List<Ranked1v1EntryBean> result = Arrays.asList(
        Ranked1v1EntryBeanBuilder.create().defaultValues().username("user1").get(),
        Ranked1v1EntryBeanBuilder.create().defaultValues().username("user2").get()
    );

    assertThat(instance.getRanked1v1Entries(), equalTo(result));
    verify(httpTransport, times(2)).buildRequest("GET", "http://api.example.com/ranked1v1");
  }

  @Test
  public void testGetRanked1v1Stats() throws Exception {
    instance.requestFactory = instance.httpTransport.createRequestFactory();
    instance.credential = mock(Credential.class);


    mockResponse("{'data': [" +
            " {" +
            "   'id': '/ranked1v1/stats'," +
            "   'attributes': {" +
            "     '100': 1," +
            "     '1200': 5," +
            "     '1400': 5" +
            "   }" +
            " }" +
            "]}",
        "{'data': []}");

    Ranked1v1Stats ranked1v1Stats = new Ranked1v1Stats();
    ranked1v1Stats.setId("/ranked1v1/stats");

    assertThat(instance.getRanked1v1Stats(), equalTo(ranked1v1Stats));
    verify(httpTransport).buildRequest("GET", "http://api.example.com/ranked1v1/stats");
  }

  @Test
  public void testGetRanked1v1EntryForPlayer() throws Exception {
    instance.requestFactory = instance.httpTransport.createRequestFactory();
    instance.credential = mock(Credential.class);


    mockResponse("{'data': [" +
            " {" +
            "   'id': '2'," +
            "   'attributes': {" +
            "     'login': 'user1'," +
            "     'num_games': 3" +
            "   }" +
            " }" +
            "]}",
        "{'data': []}");

    Ranked1v1EntryBean entry = Ranked1v1EntryBeanBuilder.create().defaultValues().username("user1").get();

    assertThat(instance.getRanked1v1EntryForPlayer(123), equalTo(entry));
    verify(httpTransport).buildRequest("GET", "http://api.example.com/ranked1v1/123");
  }
}
