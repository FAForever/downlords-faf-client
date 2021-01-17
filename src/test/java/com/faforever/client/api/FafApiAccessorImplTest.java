package com.faforever.client.api;

import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.Event;
import com.faforever.client.api.dto.Game;
import com.faforever.client.api.dto.GamePlayerStats;
import com.faforever.client.api.dto.GameReview;
import com.faforever.client.api.dto.LeaderboardRatingJournal;
import com.faforever.client.api.dto.MapVersion;
import com.faforever.client.api.dto.MapVersionReview;
import com.faforever.client.api.dto.ModVersionReview;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.api.dto.PlayerEvent;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.mod.ModVersion;
import com.faforever.client.mod.ModVersionBuilder;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FafApiAccessorImplTest {

  @Rule
  public TemporaryFolder preferencesDirectory = new TemporaryFolder();

  private FafApiAccessorImpl instance;

  @Mock
  private EventBus eventBus;
  @Mock
  private OAuth2RestTemplate restOperations;
  @Mock
  private RestTemplateBuilder restTemplateBuilder;
  @Mock
  private JsonApiMessageConverter jsonApiMessageConverter;
  @Mock
  private JsonApiErrorHandler jsonApiErrorHandler;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(restTemplateBuilder.requestFactory(any(Supplier.class))).thenReturn(restTemplateBuilder);
    when(restTemplateBuilder.additionalMessageConverters(any(JsonApiMessageConverter.class))).thenReturn(restTemplateBuilder);
    when(restTemplateBuilder.rootUri(any())).thenReturn(restTemplateBuilder);
    when(restTemplateBuilder.errorHandler(any())).thenReturn(restTemplateBuilder);
    when(restTemplateBuilder.configure(any(OAuth2RestTemplate.class))).thenReturn(restOperations);

    instance = new FafApiAccessorImpl(eventBus, restTemplateBuilder, new ClientProperties(), jsonApiMessageConverter, jsonApiErrorHandler);
    instance.afterPropertiesSet();
    instance.authorize(123, "junit", "42");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetPlayerAchievements() {
    PlayerAchievement playerAchievement1 = new PlayerAchievement();
    playerAchievement1.setId("1");
    playerAchievement1.setAchievement(new AchievementDefinition().setId("1-2-3"));
    PlayerAchievement playerAchievement2 = new PlayerAchievement();
    playerAchievement2.setId("2");
    playerAchievement2.setAchievement(new AchievementDefinition().setId("2-3-4"));
    List<PlayerAchievement> result = Arrays.asList(playerAchievement1, playerAchievement2);

    when(restOperations.getForObject(anyString(), eq(List.class)))
        .thenReturn(result)
        .thenReturn(emptyList());

    assertThat(instance.getPlayerAchievements(123), is(result));

    verify(restOperations).getForObject("/data/playerAchievement?filter=player.id==\"123\"&page[size]=10000&page[number]=1", List.class);
  }

  @Test
  public void testGetAchievementDefinitions() {
    AchievementDefinition achievementDefinition1 = new AchievementDefinition();
    achievementDefinition1.setId("1-2-3");
    AchievementDefinition achievementDefinition2 = new AchievementDefinition();
    achievementDefinition2.setId("2-3-4");
    List<AchievementDefinition> result = Arrays.asList(achievementDefinition1, achievementDefinition2);

    when(restOperations.getForObject(startsWith("/data/achievement"), eq(List.class)))
        .thenReturn(result)
        .thenReturn(emptyList());

    assertThat(instance.getAchievementDefinitions(), is(result));
  }

  @Test
  public void testGetAchievementDefinition() {
    AchievementDefinition achievementDefinition = new AchievementDefinition();
    achievementDefinition.setId("1-2-3");

    when(restOperations.getForObject(startsWith("/data/achievement/123"), eq(AchievementDefinition.class), anyMap()))
        .thenReturn(achievementDefinition);

    assertThat(instance.getAchievementDefinition("123"), is(achievementDefinition));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetPlayerEvents() {
    PlayerEvent playerEvent1 = new PlayerEvent();
    playerEvent1.setId("1");
    playerEvent1.setEvent(new Event().setId("1-1-1"));
    playerEvent1.setCurrentCount(11);
    PlayerEvent playerEvent2 = new PlayerEvent();
    playerEvent2.setId("2");
    playerEvent2.setEvent(new Event().setId("2-2-2"));
    playerEvent2.setCurrentCount(22);
    List<PlayerEvent> result = Arrays.asList(playerEvent1, playerEvent2);

    when(restOperations.getForObject(anyString(), eq(List.class)))
        .thenReturn(result)
        .thenReturn(emptyList());

    assertThat(instance.getPlayerEvents(123), is(result));

    verify(restOperations).getForObject("/data/playerEvent" +
        "?filter=player.id==\"123\"" +
        "&page[size]=10000" +
        "&page[number]=1", List.class);
  }

  @Test
  public void testGetMods() throws MalformedURLException {
    List<ModVersion> modVersions = Arrays.asList(
        ModVersionBuilder.create().defaultValues().uid("1").get(),
        ModVersionBuilder.create().defaultValues().uid("2").get()
    );

    when(restOperations.getForObject(startsWith("/data/mod"), eq(List.class)))
        .thenReturn(modVersions)
        .thenReturn(emptyList());

    assertThat(instance.getMods(), equalTo(modVersions));
  }

  @Test
  public void testGetRatingHistory() {
    List<GamePlayerStats> gamePlayerStats = Collections.singletonList(new GamePlayerStats());

    when(restOperations.getForObject(anyString(), eq(List.class)))
        .thenReturn(gamePlayerStats)
        .thenReturn(emptyList());

    List<LeaderboardRatingJournal> result = instance.getRatingJournal(123, "ladder_1v1");

    assertThat(result, is(gamePlayerStats));
    verify(restOperations).getForObject("/data/leaderboardRatingJournal?filter=gamePlayerStats.player.id==\"123\";" +
        "leaderboard.technicalName==\"ladder_1v1\"&sort=createTime&page[size]=10000&page[number]=1", List.class);
  }

  @Test
  public void testUploadMod() throws Exception {
    Path file = Files.createTempFile("foo", null);
    instance.uploadMod(file, (written, total) -> {
    });

    verify(restOperations).postForEntity(eq("/mods/upload"), anyMap(), eq(String.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testChangePassword() {
    instance.changePassword("junit", "currentPasswordHash", "newPasswordHash");

    ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
    verify(restOperations).postForEntity(eq("/users/changePassword"), captor.capture(), eq(String.class));

    Map<String, String> body = captor.getValue();
    assertThat(body.get("currentPassword"), is("currentPasswordHash"));
    assertThat(body.get("newPassword"), is("newPasswordHash"));
  }

  @Test
  public void testGetCoopMissions() {
    when(restOperations.getForObject(startsWith("/data/coopMission"), eq(List.class))).thenReturn(emptyList());

    instance.getCoopMissions();

    verify(restOperations).getForObject(eq("/data/coopMission?page[size]=10000&page[number]=1"), eq(List.class));
  }

  @Test
  public void getCoopLeaderboardAll() {
    when(restOperations.getForObject(startsWith("/data/coopResult"), eq(List.class))).thenReturn(emptyList());

    instance.getCoopLeaderboard("1", 0);

    verify(restOperations).getForObject(eq("/data/coopResult?filter=mission==\"1\"&include=game.playerStats.player&sort=duration&page[size]=1000&page[number]=1"), eq(List.class));
  }

  @Test
  public void getCoopLeaderboardOnePlayer() {
    when(restOperations.getForObject(startsWith("/data/coopResult"), eq(List.class))).thenReturn(emptyList());

    instance.getCoopLeaderboard("1", 1);

    verify(restOperations).getForObject(eq("/data/coopResult?filter=mission==\"1\";playerCount==\"1\"&include=game.playerStats.player&sort=duration&page[size]=1000&page[number]=1"), eq(List.class));
  }

  @Test
  public void testCreateGameReview() {
    GameReview gameReview = new GameReview().setGame(new Game().setId("5"));

    when(restOperations.postForEntity(eq("/data/game/5/reviews"), eq(gameReview), eq(GameReview.class)))
        .thenReturn(new ResponseEntity<>(HttpStatus.OK));

    instance.createGameReview(gameReview);

    ArgumentCaptor<GameReview> captor = ArgumentCaptor.forClass(GameReview.class);
    verify(restOperations).postForEntity(eq("/data/game/5/reviews"), captor.capture(), eq(GameReview.class));
    GameReview review = captor.getValue();

    assertThat(review, is(gameReview));
  }

  @Test
  public void testCreateModVersionReview() {
    ModVersionReview modVersionReview = new ModVersionReview();
    when(restOperations.postForEntity(eq("/data/modVersion/5/reviews"), eq(modVersionReview), eq(ModVersionReview.class)))
        .thenReturn(new ResponseEntity<>(HttpStatus.OK));

    instance.createModVersionReview(modVersionReview.setModVersion(new com.faforever.client.api.dto.ModVersion().setId("5")));

    ArgumentCaptor<ModVersionReview> captor = ArgumentCaptor.forClass(ModVersionReview.class);
    verify(restOperations).postForEntity(eq("/data/modVersion/5/reviews"), captor.capture(), eq(ModVersionReview.class));
    ModVersionReview review = captor.getValue();

    assertThat(review, is(modVersionReview));
  }

  @Test
  public void testCreateMapVersionReview() {
    MapVersionReview mapVersionReview = new MapVersionReview().setMapVersion(new MapVersion().setId("5"));
    when(restOperations.postForEntity(eq("/data/mapVersion/5/reviews"), eq(mapVersionReview), eq(MapVersionReview.class)))
        .thenReturn(new ResponseEntity<>(HttpStatus.OK));

    instance.createMapVersionReview(mapVersionReview);

    ArgumentCaptor<MapVersionReview> captor = ArgumentCaptor.forClass(MapVersionReview.class);
    verify(restOperations).postForEntity(eq("/data/mapVersion/5/reviews"), captor.capture(), eq(MapVersionReview.class));
    MapVersionReview review = captor.getValue();

    assertThat(review, is(mapVersionReview));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetLastGameOnMap() {
    when(restOperations.getForObject(startsWith("/data/game"), eq(List.class)))
        .thenReturn(Collections.singletonList(new Game()))
        .thenReturn(emptyList());

    instance.getLastGamesOnMap(4, "42", 3);

    verify(restOperations).getForObject(contains("filter=mapVersion.id==\"42\";playerStats.player.id==\"4\""), eq(List.class));
  }

  @Test
  public void testGetLatestVersionMap() {
    MapVersion localMap = new MapVersion().setFolderName("palaneum.v0001");

    com.faforever.client.api.dto.Map map = new com.faforever.client.api.dto.Map()
        .setLatestVersion(new MapVersion().setFolderName("palaneum.v0002"));
    MapVersion mapFromServer = new MapVersion().setFolderName("palaneum.v0001")
        .setMap(map);

    when(restOperations.getForObject(startsWith("/data/mapVersion"), eq(List.class)))
        .thenReturn(Collections.singletonList(mapFromServer));

    assertThat(instance.getLatestVersionMap(localMap.getFolderName()), is(Optional.of(mapFromServer)));
    String parameters = String.format("filter=filename==\"maps/%s.zip\";map.latestVersion.hidden==\"false\"", localMap.getFolderName());
    verify(restOperations).getForObject(contains(parameters), eq(List.class));
  }

  @Test
  public void testGetLatestVersionMapIfNoMapFromServer() {
    MapVersion localMap = new MapVersion().setFolderName("palaneum.v0001__1"); // the map does not exist on server

    when(restOperations.getForObject(startsWith("/data/mapVersion"), eq(List.class)))
        .thenReturn(emptyList());

    assertThat(instance.getLatestVersionMap(localMap.getFolderName()), is(Optional.empty()));
    String parameters = String.format("filter=filename==\"maps/%s.zip\";map.latestVersion.hidden==\"false\"", localMap.getFolderName());
    verify(restOperations).getForObject(contains(parameters), eq(List.class));
  }
}
