package com.faforever.client.api;

import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.mod.ModInfoBean;
import com.faforever.client.mod.ModInfoBeanBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.UserService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import javafx.beans.property.SimpleBooleanProperty;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static com.faforever.client.net.UriStartingWithMatcher.uriStartingWith;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FafApiAccessorImplTest {

  @Rule
  public TemporaryFolder preferencesDirectory = new TemporaryFolder();
  private FafApiAccessorImpl instance;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private LowLevelHttpRequest httpRequest;
  @Mock
  private LowLevelHttpResponse lowLevelHttpResponse;
  @Mock
  private UserService userService;
  @Mock
  private ClientHttpRequestFactory clientHttpRequestFactory;
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
    instance.oAuthUrl = "http://api.example.com/oauth/authorize";
    instance.oAuthLoginUrl = new URI("http://api.example.com/login");
    instance.httpTransport = httpTransport;
    instance.userService = userService;
    instance.clientHttpRequestFactory = clientHttpRequestFactory;
    instance.jsonFactory = new GsonFactory();

    SimpleBooleanProperty loggedInProperty = new SimpleBooleanProperty();

    when(preferencesService.getPreferencesDirectory()).thenReturn(preferencesDirectory.getRoot().toPath());
    when(userService.loggedInProperty()).thenReturn(loggedInProperty);
    when(userService.getUid()).thenReturn(123);
    when(userService.getUsername()).thenReturn("junit");
    when(userService.getPassword()).thenReturn("42");

    when(lowLevelHttpResponse.getStatusCode()).thenReturn(200);
    when(httpRequest.execute()).thenReturn(lowLevelHttpResponse);

    instance.postConstruct();
    authorize();
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
    verify(httpTransport).buildRequest("GET", "http://api.example.com/players/123/achievements?page%5Bnumber%5D=1");
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
    verify(httpTransport).buildRequest("GET", "http://api.example.com/achievements?sort=order&page%5Bnumber%5D=1");
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
    verify(httpTransport).buildRequest("GET", "http://api.example.com/players/123/events?page%5Bnumber%5D=1");
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
            "     'version': '1'," +
            "     'download_url': 'http://example.com/mod1.zip'" +
            "   }" +
            " }," +
            " {" +
            "   'id': '2'," +
            "   'attributes': {" +
            "     'create_time': '2011-12-03T10:15:30'," +
            "     'version': '1'," +
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
    verify(httpTransport).buildRequest("GET", "http://api.example.com/mods?page%5Bnumber%5D=1");
    verify(httpTransport).buildRequest("GET", "http://api.example.com/mods?page%5Bnumber%5D=2");
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
    verify(httpTransport).buildRequest("GET", "http://api.example.com/leaderboards/1v1?page%5Bnumber%5D=1");
    verify(httpTransport).buildRequest("GET", "http://api.example.com/leaderboards/1v1?page%5Bnumber%5D=2");
  }

  @Test
  public void testGetRanked1v1Stats() throws Exception {
    instance.requestFactory = instance.httpTransport.createRequestFactory();
    instance.credential = mock(Credential.class);


    mockResponse("{'data': [" +
            " {" +
            "   'id': '/leaderboards/1v1/stats'," +
            "   'attributes': {" +
            "     '100': 1," +
            "     '1200': 5," +
            "     '1400': 5" +
            "   }" +
            " }" +
            "]}",
        "{'data': []}");

    Ranked1v1Stats ranked1v1Stats = new Ranked1v1Stats();
    ranked1v1Stats.setId("/leaderboards/1v1/stats");

    assertThat(instance.getRanked1v1Stats(), equalTo(ranked1v1Stats));
    verify(httpTransport).buildRequest("GET", "http://api.example.com/leaderboards/1v1/stats");
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
    verify(httpTransport).buildRequest("GET", "http://api.example.com/leaderboards/1v1/123");
  }

  @Test
  public void testGetRatingHistoryGlobal() throws Exception {
    instance.requestFactory = instance.httpTransport.createRequestFactory();
    instance.credential = mock(Credential.class);

    mockResponse("{" +
            "  'data': {" +
            "    'attributes': {" +
            "      'history': {" +
            "        '1469921413': [1026.62, 49.4094]," +
            "        '1469989967': [1024.01, 49.4545]," +
            "        '1470842200': [1020.65, 50.1963]" +
            "      }" +
            "    }," +
            "    'id': '21447'," +
            "    'type': 'leaderboard_history'" +
            "  }" +
            "}",
        "{'data': []}");

    History ratingHistory = instance.getRatingHistory(RatingType.GLOBAL, 123);

    verify(httpTransport).buildRequest("GET", "http://api.example.com/players/123/ratings/global/history");
    assertThat(ratingHistory.getData().values(), hasSize(3));
    assertThat(ratingHistory.getData().get("1469921413").get(0), is(1026.62f));
    assertThat(ratingHistory.getData().get("1469921413").get(1), is(49.4094f));
  }

  @Test
  public void testGetRatingHistory1v1() throws Exception {
    instance.requestFactory = instance.httpTransport.createRequestFactory();
    instance.credential = mock(Credential.class);

    mockResponse("{" +
            "  'data': {" +
            "    'attributes': {" +
            "      'history': {" +
            "        '1469921413': [1026.62, 49.4094]," +
            "        '1469989967': [1024.01, 49.4545]," +
            "        '1470842200': [1020.65, 50.1963]" +
            "      }" +
            "    }," +
            "    'id': '21447'," +
            "    'type': 'leaderboard_history'" +
            "  }" +
            "}",
        "{'data': []}");

    History ratingHistory = instance.getRatingHistory(RatingType.LADDER_1V1, 123);

    verify(httpTransport).buildRequest("GET", "http://api.example.com/players/123/ratings/1v1/history");
    assertThat(ratingHistory.getData().values(), hasSize(3));
    assertThat(ratingHistory.getData().get("1469921413").get(0), is(1026.62f));
    assertThat(ratingHistory.getData().get("1469921413").get(1), is(49.4094f));
  }

  @Test
  public void testUploadMod() throws Exception {
    instance.requestFactory = instance.httpTransport.createRequestFactory();
    instance.credential = mock(Credential.class);

    Path file = Files.createTempFile("foo", null);

    // FIXME filename
    instance.uploadMod(file, (written, total) -> {
    });

    verify(httpTransport).buildRequest("POST", "http://api.example.com/mods/upload");
  }

  @Test
  public void testChangePassword() throws Exception {
    instance.requestFactory = instance.httpTransport.createRequestFactory();
    instance.credential = mock(Credential.class);

    instance.changePassword("currentPasswordHash", "newPasswordHash");

    verify(httpTransport).buildRequest("POST", "http://api.example.com/users/change_password");
  }

  private void authorize() throws Exception {
    when(userService.getUsername()).thenReturn("junit");
    when(userService.getPassword()).thenReturn("junit-password");

    ClientHttpResponse loginResponse = new MockClientHttpResponse((byte[]) null, HttpStatus.FOUND);
    loginResponse.getHeaders().add("Set-Cookie", "some cookies");
    MockClientHttpRequest loginRequest = new MockClientHttpRequest();
    loginRequest.setResponse(loginResponse);
    when(clientHttpRequestFactory.createRequest(instance.oAuthLoginUrl, HttpMethod.POST)).thenReturn(loginRequest);

    ClientHttpResponse authResponse = new MockClientHttpResponse((byte[]) null, HttpStatus.FOUND);
    authResponse.getHeaders().setLocation(new URI("http://localhost:1111?code=1337"));
    MockClientHttpRequest authRequest = new MockClientHttpRequest();
    authRequest.setResponse(authResponse);
    when(clientHttpRequestFactory.createRequest(uriStartingWith(instance.oAuthUrl), eq(HttpMethod.POST))).thenReturn(authRequest);

    mockResponse("{}");

    instance.authorize(123);

    assertThat(instance.credential, notNullValue());
  }

  private static class SpyableHttpTransport extends HttpTransport {
    LowLevelHttpRequest lowLevelHttpRequest;

    @Override
    public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
      return lowLevelHttpRequest;
    }
  }
}
