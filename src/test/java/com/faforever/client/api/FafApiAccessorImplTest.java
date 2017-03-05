package com.faforever.client.api;

import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.GamePlayerStats;
import com.faforever.client.api.dto.Ladder1v1LeaderboardEntry;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.api.dto.PlayerEvent;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.leaderboard.LeaderboardEntry;
import com.faforever.client.mod.Mod;
import com.faforever.client.mod.ModInfoBeanBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.UserService;
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
  private PreferencesService preferencesService;
  @Mock
  private UserService userService;
  @Mock
  private EventBus eventBus;
  @Mock
  private OAuth2RestTemplate restOperations;
  @Mock
  private RestTemplateBuilder restTemplateBuilder;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(restTemplateBuilder.configure(any(OAuth2RestTemplate.class))).thenReturn(restOperations);

    instance = new FafApiAccessorImpl(eventBus, restTemplateBuilder, new ClientProperties());
    instance.postConstruct();
    instance.authorize(123, "junit", "42");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetPlayerAchievements() throws Exception {
    PlayerAchievement playerAchievement1 = new PlayerAchievement();
    playerAchievement1.setId("1");
    playerAchievement1.setAchievementId("1-2-3");
    PlayerAchievement playerAchievement2 = new PlayerAchievement();
    playerAchievement2.setId("2");
    playerAchievement2.setAchievementId("2-3-4");
    List<PlayerAchievement> result = Arrays.asList(playerAchievement1, playerAchievement2);

    when(restOperations.getForObject(anyString(), eq(List.class)))
        .thenReturn(result)
        .thenReturn(emptyList());

    assertThat(instance.getPlayerAchievements(123), is(result));

    verify(restOperations).getForObject("/data/playerAchievement?filter[playerAchievement.player.id]=123&page[size]=10000&page[number]=1", List.class);
    verify(restOperations).getForObject("/data/playerAchievement?filter[playerAchievement.player.id]=123&page[size]=10000&page[number]=2", List.class);
  }

  @Test
  public void testGetAchievementDefinitions() throws Exception {
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
  public void testGetAchievementDefinition() throws Exception {
    AchievementDefinition achievementDefinition = new AchievementDefinition();
    achievementDefinition.setId("1-2-3");

    when(restOperations.getForObject(startsWith("/data/achievement/123"), eq(AchievementDefinition.class), anyMap()))
        .thenReturn(achievementDefinition);

    assertThat(instance.getAchievementDefinition("123"), is(achievementDefinition));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetPlayerEvents() throws Exception {
    PlayerEvent playerEvent1 = new PlayerEvent();
    playerEvent1.setId("1");
    playerEvent1.setEventId("1-1-1");
    playerEvent1.setCount(11);
    PlayerEvent playerEvent2 = new PlayerEvent();
    playerEvent2.setId("2");
    playerEvent2.setEventId("2-2-2");
    playerEvent2.setCount(22);
    List<PlayerEvent> result = Arrays.asList(playerEvent1, playerEvent2);

    when(restOperations.getForObject(anyString(), eq(List.class)))
        .thenReturn(result)
        .thenReturn(emptyList());

    assertThat(instance.getPlayerEvents(123), is(result));

    verify(restOperations).getForObject("/data/playerEvent" +
        "?filter[playerEvent.player.id]=123" +
        "&page[size]=10000" +
        "&page[number]=1", List.class);
  }

  @Test
  public void testGetMods() throws Exception {
    List<Mod> mods = Arrays.asList(
        ModInfoBeanBuilder.create().defaultValues().uid("1").get(),
        ModInfoBeanBuilder.create().defaultValues().uid("2").get()
    );

    when(restOperations.getForObject(startsWith("/data/mod"), eq(List.class)))
        .thenReturn(mods)
        .thenReturn(emptyList());

    assertThat(instance.getMods(), equalTo(mods));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetLadder1v1Leaderboard() throws Exception {
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
  public void testGetLadder1v1EntryForPlayer() throws Exception {
    Ladder1v1LeaderboardEntry entry = new Ladder1v1LeaderboardEntry();
    when(restOperations.getForObject("/leaderboards/ladder1v1/123", Ladder1v1LeaderboardEntry.class, emptyMap())).thenReturn(entry);

    assertThat(instance.getLadder1v1EntryForPlayer(123), equalTo(entry));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetFafGamePlayerStats() throws Exception {
    List<GamePlayerStats> gamePlayerStats = Collections.singletonList(new GamePlayerStats());

    when(restOperations.getForObject(anyString(), eq(List.class)))
        .thenReturn(gamePlayerStats)
        .thenReturn(emptyList());

    List<GamePlayerStats> result = instance.getGamePlayerStats(123, KnownFeaturedMod.FAF);

    assertThat(result, is(gamePlayerStats));
    verify(restOperations).getForObject("/data/gamePlayerStats?filter[gamePlayerStats.game.featuredMod.technicalName]=faf&filter[gamePlayerStats.player.id]=123&page[size]=10000&page[number]=1", List.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetRatingHistory1v1() throws Exception {
    List<GamePlayerStats> gamePlayerStats = Collections.singletonList(new GamePlayerStats());

    when(restOperations.getForObject(anyString(), eq(List.class)))
        .thenReturn(gamePlayerStats)
        .thenReturn(emptyList());

    List<GamePlayerStats> result = instance.getGamePlayerStats(123, KnownFeaturedMod.LADDER_1V1);

    assertThat(result, is(gamePlayerStats));
    verify(restOperations).getForObject("/data/gamePlayerStats" +
        "?filter[gamePlayerStats.game.featuredMod.technicalName]=ladder1v1" +
        "&filter[gamePlayerStats.player.id]=123" +
        "&page[size]=10000" +
        "&page[number]=1", List.class);
  }

  @Test
  public void testUploadMod() throws Exception {
    ResponseEntity<Void> expected = new ResponseEntity<>(HttpStatus.OK);
    when(restOperations.postForEntity(eq("/mods/upload"), anyMap(), eq(Void.class)))
        .thenReturn(expected);

    Path file = Files.createTempFile("foo", null);
    instance.uploadMod(file, (written, total) -> {
    });

    verify(restOperations).postForEntity(eq("/mods/upload"), anyMap(), eq(Void.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testChangePassword() throws Exception {
    ResponseEntity<Void> expected = new ResponseEntity<>(HttpStatus.OK);
    when(restOperations.postForEntity(eq("/users/change_password"), anyMap(), eq(Void.class)))
        .thenReturn(expected);

    instance.changePassword("junit", "currentPasswordHash", "newPasswordHash");

    ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
    verify(restOperations).postForEntity(eq("/users/change_password"), captor.capture(), eq(Void.class));

    Map<String, String> body = captor.getValue();
    assertThat(body.get("name"), is("junit"));
    assertThat(body.get("pw_hash_old"), is("currentPasswordHash"));
    assertThat(body.get("pw_hash_new"), is("newPasswordHash"));
  }

  @Test
  public void testGetCoopMissions() throws Exception {
    when(restOperations.getForObject(startsWith("/data/coopMission"), eq(List.class))).thenReturn(emptyList());

    instance.getCoopMissions();

    verify(restOperations).getForObject(eq("/data/coopMission?page[size]=10000&page[number]=1"), eq(List.class));
  }
}
