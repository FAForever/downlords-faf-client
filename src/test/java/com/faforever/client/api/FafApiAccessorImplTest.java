package com.faforever.client.api;

import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.Event;
import com.faforever.client.api.dto.Game;
import com.faforever.client.api.dto.GamePlayerStats;
import com.faforever.client.api.dto.GameReview;
import com.faforever.client.api.dto.Ladder1v1LeaderboardEntry;
import com.faforever.client.api.dto.MapVersion;
import com.faforever.client.api.dto.MapVersionReview;
import com.faforever.client.api.dto.ModVersionReview;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.api.dto.PlayerEvent;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.leaderboard.LeaderboardEntry;
import com.faforever.client.mod.ModInfoBeanBuilder;
import com.faforever.client.mod.ModVersion;
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
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
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

    when(restTemplateBuilder.requestFactory(any(ClientHttpRequestFactory.class))).thenReturn(restTemplateBuilder);
    when(restTemplateBuilder.additionalMessageConverters(any(JsonApiMessageConverter.class))).thenReturn(restTemplateBuilder);
    when(restTemplateBuilder.rootUri(any())).thenReturn(restTemplateBuilder);
    when(restTemplateBuilder.errorHandler(any())).thenReturn(restTemplateBuilder);
    when(restTemplateBuilder.configure(any(OAuth2RestTemplate.class))).thenReturn(restOperations);

    instance = new FafApiAccessorImpl(eventBus, restTemplateBuilder, new ClientProperties(), jsonApiMessageConverter, jsonApiErrorHandler);
    instance.postConstruct();
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
  public void testGetMods() {
    List<ModVersion> modVersions = Arrays.asList(
        ModInfoBeanBuilder.create().defaultValues().uid("1").get(),
        ModInfoBeanBuilder.create().defaultValues().uid("2").get()
    );

    when(restOperations.getForObject(startsWith("/data/mod"), eq(List.class)))
        .thenReturn(modVersions)
        .thenReturn(emptyList());

    assertThat(instance.getMods(), equalTo(modVersions));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetLadder1v1Leaderboard() {
    List<LeaderboardEntry> result = Arrays.asList(
        Ladder1v1EntryBeanBuilder.create().defaultValues().username("user1").get(),
        Ladder1v1EntryBeanBuilder.create().defaultValues().username("user2").get()
    );

    ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass(Map.class);
    when(restOperations.getForObject(eq("/leaderboards/ladder1v1"), eq(List.class), captor.capture()))
        .thenReturn(result)
        .thenReturn(emptyList());

    assertThat(instance.getLadder1v1Leaderboard(), equalTo(result));

    Map<String, ?> params = captor.getValue();
    assertThat(params.get("sort"), is("-rating"));
    assertThat(params.get("include"), is("player"));
    assertThat(params.get("fields[ladder1v1Rating]"), is("rating,numGames,winGames"));
    assertThat(params.get("fields[player]"), is("login"));
  }

  @Test
  public void testGetLadder1v1EntryForPlayer() {
    Ladder1v1LeaderboardEntry entry = new Ladder1v1LeaderboardEntry();
    when(restOperations.getForObject("/leaderboards/ladder1v1/123", Ladder1v1LeaderboardEntry.class, emptyMap())).thenReturn(entry);

    assertThat(instance.getLadder1v1EntryForPlayer(123), equalTo(entry));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetFafGamePlayerStats() {
    List<GamePlayerStats> gamePlayerStats = Collections.singletonList(new GamePlayerStats());

    when(restOperations.getForObject(anyString(), eq(List.class)))
        .thenReturn(gamePlayerStats)
        .thenReturn(emptyList());

    List<GamePlayerStats> result = instance.getGamePlayerStats(123, KnownFeaturedMod.FAF);

    assertThat(result, is(gamePlayerStats));
    verify(restOperations).getForObject("/data/gamePlayerStats" +
        "?filter=player.id==\"123\";game.featuredMod.technicalName==\"faf\"" +
        "&page[size]=10000" +
        "&page[number]=1", List.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetRatingHistory1v1() {
    List<GamePlayerStats> gamePlayerStats = Collections.singletonList(new GamePlayerStats());

    when(restOperations.getForObject(anyString(), eq(List.class)))
        .thenReturn(gamePlayerStats)
        .thenReturn(emptyList());

    List<GamePlayerStats> result = instance.getGamePlayerStats(123, KnownFeaturedMod.LADDER_1V1);

    assertThat(result, is(gamePlayerStats));
    verify(restOperations).getForObject("/data/gamePlayerStats" +
        "?filter=player.id==\"123\";game.featuredMod.technicalName==\"ladder1v1\"" +
        "&page[size]=10000" +
        "&page[number]=1", List.class);
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

    verify(restOperations).getForObject("/data/game?filter=mapVersion.id==\"42\";playerStats.player.id==\"4\"&sort=-endTime&page[size]=3&page[number]=1", List.class);
  }
}
