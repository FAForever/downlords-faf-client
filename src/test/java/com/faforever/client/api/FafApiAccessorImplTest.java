package com.faforever.client.api;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.JsonApiConfig;
import com.faforever.client.reporting.ModerationReportBuilder;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.CoopMission;
import com.faforever.commons.api.dto.CoopResult;
import com.faforever.commons.api.dto.Event;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.api.dto.GameReview;
import com.faforever.commons.api.dto.LeaderboardRatingJournal;
import com.faforever.commons.api.dto.MapVersion;
import com.faforever.commons.api.dto.MapVersionReview;
import com.faforever.commons.api.dto.Mod;
import com.faforever.commons.api.dto.ModVersionReview;
import com.faforever.commons.api.dto.ModerationReport;
import com.faforever.commons.api.dto.Player;
import com.faforever.commons.api.dto.PlayerAchievement;
import com.faforever.commons.api.dto.PlayerEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;
import com.google.common.eventbus.EventBus;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriUtils;
import org.testfx.util.WaitForAsyncUtils;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FafApiAccessorImplTest extends ServiceTest {

  private FafApiAccessorImpl instance;

  @Mock
  private EventBus eventBus;
  @Mock
  private OAuthTokenFilter oAuthTokenFilter;

  private ResourceConverter resourceConverter;
  private MockWebServer mockApi;

  @AfterEach
  public void killServer() throws IOException {
    mockApi.shutdown();
  }

  @BeforeEach
  public void setUp() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    resourceConverter = new JsonApiConfig().resourceConverter(objectMapper);
    JsonApiReader jsonApiReader = new JsonApiReader(resourceConverter);
    JsonApiWriter jsonApiWriter = new JsonApiWriter(resourceConverter);
    mockApi = new MockWebServer();
    mockApi.start();

    ClientProperties clientProperties = new ClientProperties();
    clientProperties.getApi().setBaseUrl(String.format("http://localhost:%s", mockApi.getPort()));
    instance = new FafApiAccessorImpl(eventBus, clientProperties, jsonApiReader, jsonApiWriter, oAuthTokenFilter);
    instance.afterPropertiesSet();
    instance.authorize();
  }

  private void prepareJsonApiResponse(Object object) throws Exception {
    byte[] serializedObject;
    if (object instanceof Iterable) {
      serializedObject = resourceConverter.writeDocumentCollection(new JSONAPIDocument<Iterable<?>>((Iterable<?>) object));
    } else if (object instanceof JSONAPIDocument) {
      serializedObject = resourceConverter.writeDocument((JSONAPIDocument<?>) object);
    } else {
      serializedObject = resourceConverter.writeDocument(new JSONAPIDocument<>(object));
    }
    mockApi.enqueue(new MockResponse()
        .setBody(new String(serializedObject))
        .addHeader("Content-Type", "application/vnd.api+json;charset=utf-8"));
  }

  @Test
  public void testGetPlayerAchievements() throws Exception {
    List<PlayerAchievement> result = List.of(
        (PlayerAchievement) new PlayerAchievement()
            .setAchievement(new AchievementDefinition().setId("1-2-3"))
            .setId("1"),
        (PlayerAchievement) new PlayerAchievement()
            .setAchievement(new AchievementDefinition().setId("2-3-4"))
            .setId("2")
    );

    prepareJsonApiResponse(result);

    StepVerifier.create(instance.getPlayerAchievements(123))
        .expectNext(result.toArray(PlayerAchievement[]::new))
        .verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.GET.name(), recordedRequest.getMethod());
    assertEquals("/data/playerAchievement?filter=player.id==\"123\"&page[size]=10000&page[number]=1", UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void testGetAchievementDefinitions() throws Exception {
    List<AchievementDefinition> result = List.of(
        new AchievementDefinition().setId("1-2-3"),
        new AchievementDefinition().setId("2-3-4")
    );

    prepareJsonApiResponse(result);

    StepVerifier.create(instance.getAchievementDefinitions())
        .expectNext(result.toArray(AchievementDefinition[]::new))
        .verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.GET.name(), recordedRequest.getMethod());
    assertEquals("/data/achievement?sort=order&page[size]=10000&page[number]=1", UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void testGetAchievementDefinition() throws Exception {
    AchievementDefinition achievementDefinition = new AchievementDefinition();
    achievementDefinition.setId("1-2-3");

    prepareJsonApiResponse(List.of(achievementDefinition));

    StepVerifier.create(instance.getAchievementDefinitions())
        .expectNext(achievementDefinition)
        .verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.GET.name(), recordedRequest.getMethod());
    assertEquals("/data/achievement?sort=order&page[size]=10000&page[number]=1", UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void testGetPlayerEvents() throws Exception {
    List<PlayerEvent> result = List.of(
        new PlayerEvent()
            .setEvent(new Event().setId("1-1-1"))
            .setCurrentCount(11)
            .setId("1"),
        new PlayerEvent()
            .setEvent(new Event().setId("2-2-2"))
            .setCurrentCount(22)
            .setId("2")
    );

    prepareJsonApiResponse(result);

    StepVerifier.create(instance.getPlayerEvents(123))
        .expectNext(result.toArray(PlayerEvent[]::new))
        .verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.GET.name(), recordedRequest.getMethod());
    assertEquals("/data/playerEvent?filter=player.id==\"123\"&page[size]=10000&page[number]=1", UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void testGetMods() throws Exception {
    List<Mod> result = List.of(
        (Mod) new Mod().setId("1"),
        (Mod) new Mod().setId("2")
    );

    prepareJsonApiResponse(result);

    StepVerifier.create(instance.getMods())
        .expectNext(result.toArray(Mod[]::new))
        .verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.GET.name(), recordedRequest.getMethod());
    assertEquals("/data/mod?include=latestVersion,reviewsSummary,versions,versions.reviews,versions.reviews.player&page[size]=10000&page[number]=1", UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void testGetRatingHistory() throws Exception {
    List<LeaderboardRatingJournal> result = List.of((LeaderboardRatingJournal) new LeaderboardRatingJournal().setId("1"));

    prepareJsonApiResponse(result);

    StepVerifier.create(instance.getRatingJournal(123, 1))
        .expectNext(result.toArray(LeaderboardRatingJournal[]::new))
        .verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.GET.name(), recordedRequest.getMethod());
    assertEquals("/data/leaderboardRatingJournal?filter=gamePlayerStats.player.id==\"123\";leaderboard.id==\"1\"&include=gamePlayerStats&page[size]=10000&page[number]=1", UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void testQueryPlayerByName() throws Exception {
    Player player = (Player) new Player().setId("1");

    prepareJsonApiResponse(List.of(player));

    StepVerifier.create(instance.queryPlayerByName("junit"))
        .expectNext(player)
        .verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.GET.name(), recordedRequest.getMethod());
    assertEquals("/data/player?filter=login==\"junit\"&include=names&page[size]=10000&page[number]=1", UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void testGetPlayerModerationReports() throws Exception {
    List<ModerationReport> result = List.of((ModerationReport) new ModerationReport().setId("1"));

    prepareJsonApiResponse(result);

    StepVerifier.create(instance.getPlayerModerationReports(123))
        .expectNext(result.toArray(ModerationReport[]::new))
        .verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.GET.name(), recordedRequest.getMethod());
    assertEquals("/data/moderationReport?filter=reporter.id==\"123\"&include=reporter,lastModerator,reportedUsers,game", UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void testPostModerationReport() throws Exception {
    com.faforever.client.reporting.ModerationReport report = ModerationReportBuilder.create().defaultValues().get();

    ModerationReport moderationReport = (ModerationReport) new ModerationReport().setId("1");
    prepareJsonApiResponse(moderationReport);

    StepVerifier.create(instance.postModerationReport(report)).expectNext(moderationReport).verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.POST.name(), recordedRequest.getMethod());
    assertEquals("/data/moderationReport", UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void testUploadMod() throws Exception {
    Path file = Files.createTempFile("foo", null);

    prepareJsonApiResponse(null);

    StepVerifier.create(instance.uploadMod(file, (written, total) -> {
    })).verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.POST.name(), recordedRequest.getMethod());
    assertEquals("/mods/upload", UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void testGetCoopMissions() throws Exception {
    List<CoopMission> result = List.of(new CoopMission().setId("1"));

    prepareJsonApiResponse(result);

    StepVerifier.create(instance.getCoopMissions())
        .expectNext(result.toArray(CoopMission[]::new))
        .verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.GET.name(), recordedRequest.getMethod());
    assertEquals("/data/coopMission?page[size]=10000&page[number]=1", UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void getCoopLeaderboardAll() throws Exception {
    List<CoopResult> result = List.of(new CoopResult().setId("1"));

    prepareJsonApiResponse(result);

    StepVerifier.create(instance.getCoopLeaderboard("1", 0))
        .expectNext(result.toArray(CoopResult[]::new))
        .verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.GET.name(), recordedRequest.getMethod());
    assertEquals("/data/coopResult?filter=mission==\"1\"&include=game.playerStats.player&sort=duration&page[size]=1000&page[number]=1", UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void testCreateGameReview() throws Exception {
    GameReview gameReview = new GameReview().setGame(new Game().setId("5"));

    prepareJsonApiResponse(gameReview.setId("1"));

    StepVerifier.create(instance.createGameReview(gameReview))
        .expectNext(gameReview)
        .verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.POST.name(), recordedRequest.getMethod());
    assertEquals("/data/game/5/reviews", UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void testCreateModVersionReview() throws Exception {
    ModVersionReview modVersionReview = new ModVersionReview()
        .setModVersion((com.faforever.commons.api.dto.ModVersion) new com.faforever.commons.api.dto.ModVersion().setId("5"));

    prepareJsonApiResponse(modVersionReview.setId("1"));

    StepVerifier.create(instance.createModVersionReview(modVersionReview))
        .expectNext(modVersionReview)
        .verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.POST.name(), recordedRequest.getMethod());
    assertEquals("/data/modVersion/5/reviews", UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void testCreateMapVersionReview() throws Exception {
    MapVersionReview mapVersionReview = new MapVersionReview()
        .setMapVersion((MapVersion) new MapVersion().setId("5"));

    prepareJsonApiResponse(mapVersionReview.setId("1"));

    StepVerifier.create(instance.createMapVersionReview(mapVersionReview))
        .expectNext(mapVersionReview)
        .verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.POST.name(), recordedRequest.getMethod());
    assertEquals("/data/mapVersion/5/reviews", UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetLastGameOnMap() throws Exception {
    List<Game> result = List.of(new Game().setId("1"));

    prepareJsonApiResponse(result);

    StepVerifier.create(instance.getLastGamesOnMap(4, "42", 3))
        .expectNext(result.toArray(Game[]::new))
        .verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.GET.name(), recordedRequest.getMethod());
    assertEquals("/data/game?filter=mapVersion.id==\"42\";playerStats.player.id==\"4\"&include=featuredMod,playerStats,playerStats.player,playerStats.ratingChanges,reviews,reviews.player,mapVersion,mapVersion.map,reviewsSummary&sort=-endTime&page[size]=3&page[number]=1", UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void testGetLatestVersionMap() throws Exception {
    MapVersion localMap = new MapVersion().setFolderName("palaneum.v0001");

    MapVersion latestVersion = new MapVersion().setFolderName("palaneum.v0002");
    com.faforever.commons.api.dto.Map map = new com.faforever.commons.api.dto.Map()
        .setLatestVersion(latestVersion);

    MapVersion mapFromServer = (MapVersion) new MapVersion().setFolderName("palaneum.v0001")
        .setMap(map).setId("1");

    List<MapVersion> result = List.of(mapFromServer);

    prepareJsonApiResponse(result);

    StepVerifier.create(instance.getMapLatestVersion(localMap.getFolderName()))
        .expectNext(result.toArray(MapVersion[]::new))
        .verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.GET.name(), recordedRequest.getMethod());
    assertEquals(String.format("/data/mapVersion?filter=filename==\"maps/%s.zip\";map.latestVersion.hidden==\"false\"&include=map,map.latestVersion,map.author,map.reviewsSummary,map.versions.reviews,map.versions.reviews.player&page[size]=1&page[number]=1", localMap.getFolderName()), UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void testGetLatestVersionMapIfNoMapFromServer() throws Exception {
    MapVersion localMap = new MapVersion().setFolderName("palaneum.v0001__1"); // the map does not exist on server

    List<MapVersion> result = List.of();

    prepareJsonApiResponse(result);

    StepVerifier.create(instance.getMapLatestVersion(localMap.getFolderName()))
        .expectNext(result.toArray(MapVersion[]::new))
        .verifyComplete();

    RecordedRequest recordedRequest = mockApi.takeRequest();

    assertEquals(HttpMethod.GET.name(), recordedRequest.getMethod());
    assertEquals(String.format("/data/mapVersion?filter=filename==\"maps/palaneum.v0001__1.zip\";map.latestVersion.hidden==\"false\"&include=map,map.latestVersion,map.author,map.reviewsSummary,map.versions.reviews,map.versions.reviews.player&page[size]=1&page[number]=1", localMap.getFolderName()), UriUtils.decode(recordedRequest.getPath(), StandardCharsets.UTF_8));
  }

  @Test
  public void testSessionExpired() {
    instance.onSessionExpiredEvent(new SessionExpiredEvent());
    RuntimeException exception = assertThrows(RuntimeException.class, () -> WaitForAsyncUtils.waitForAsync(1000, () -> instance.getMe()));
    assertEquals(TimeoutException.class, exception.getCause().getClass());
  }
}
