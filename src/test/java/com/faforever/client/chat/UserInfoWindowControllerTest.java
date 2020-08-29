package com.faforever.client.chat;

import com.faforever.client.achievements.AchievementDefinitionBuilder;
import com.faforever.client.achievements.AchievementItemController;
import com.faforever.client.achievements.AchievementService;
import com.faforever.client.achievements.PlayerAchievementBuilder;
import com.faforever.client.api.dto.AchievementState;
import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.events.EventService;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardEntry;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.stats.StatisticsService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import javafx.scene.layout.HBox;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserInfoWindowControllerTest extends AbstractPlainJavaFxTest {

  private static final String PLAYER_NAME = "junit";
  private static final int PLAYER_ID = 123;

  private UserInfoWindowController instance;

  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private I18n i18n;
  @Mock
  private StatisticsService statisticsService;
  @Mock
  private AchievementService achievementService;
  @Mock
  private EventService eventService;
  @Mock
  private UiService uiService;
  @Mock
  private AchievementItemController achievementItemController;
  @Mock
  private TimeService timeService;
  @Mock
  private PlayerService playerService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private LeaderboardService leaderboardService;

  @Before
  public void setUp() throws Exception {
    instance = new UserInfoWindowController(statisticsService, countryFlagService, achievementService, eventService,
        i18n, uiService, timeService, playerService, notificationService, leaderboardService);

    when(uiService.loadFxml("theme/achievement_item.fxml")).thenReturn(achievementItemController);
    when(achievementItemController.getRoot()).thenReturn(new HBox());
    when(playerService.getPlayersByIds(any())).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    when(i18n.get("userInfo.ratingHistory.allTime")).thenReturn("All Time");
    when(i18n.get("userInfo.ratingHistory.lastYear")).thenReturn("Last Year");
    when(i18n.get("userInfo.ratingHistory.lastMonth")).thenReturn("Last Month");
    when(i18n.get("userInfo.ratingHistory.global")).thenReturn("Global");
    when(i18n.get("userInfo.ratingHistory.1v1")).thenReturn("1v1");

    when(leaderboardService.getEntryForPlayer(eq(PLAYER_ID))).thenReturn(CompletableFuture.completedFuture(new LeaderboardEntry()));
    when(statisticsService.getRatingHistory(any(), eq(PLAYER_ID))).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        new RatingHistoryDataPoint(OffsetDateTime.now(), 1500f, 50f),
        new RatingHistoryDataPoint(OffsetDateTime.now().plus(1, ChronoUnit.DAYS), 1500f, 50f)
    )));

    loadFxml("theme/user_info_window.fxml", clazz -> instance);
  }

  @Test
  public void testSetPlayerInfoBeanNoAchievementUnlocked() throws Exception {
    when(achievementService.getAchievementDefinitions()).thenReturn(CompletableFuture.completedFuture(singletonList(
        AchievementDefinitionBuilder.create().defaultValues().get()
    )));
    when(uiService.loadFxml("theme/achievement_item.fxml")).thenReturn(achievementItemController);
    when(achievementService.getPlayerAchievements(PLAYER_ID)).thenReturn(CompletableFuture.completedFuture(
        singletonList(PlayerAchievementBuilder.create().defaultValues().get())
    ));
    when(eventService.getPlayerEvents(PLAYER_ID)).thenReturn(CompletableFuture.completedFuture(new HashMap<>()));

    instance.setPlayer(PlayerBuilder.create(PLAYER_NAME).id(PLAYER_ID).get());

    verify(achievementService).getAchievementDefinitions();
    verify(achievementService).getPlayerAchievements(PLAYER_ID);
    verify(eventService).getPlayerEvents(PLAYER_ID);

    assertThat(instance.mostRecentAchievementPane.isVisible(), is(false));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.userInfoRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testSetPlayerInfoBean() throws Exception {
    when(achievementService.getAchievementDefinitions()).thenReturn(CompletableFuture.completedFuture(asList(
        AchievementDefinitionBuilder.create().id("foo-bar").get(),
        AchievementDefinitionBuilder.create().defaultValues().get()
    )));
    when(uiService.loadFxml("theme/achievement_item.fxml")).thenReturn(achievementItemController);
    when(achievementService.getPlayerAchievements(PLAYER_ID)).thenReturn(CompletableFuture.completedFuture(asList(
        PlayerAchievementBuilder.create().defaultValues().achievementId("foo-bar").state(AchievementState.UNLOCKED).get(),
        PlayerAchievementBuilder.create().defaultValues().get()
    )));
    when(eventService.getPlayerEvents(PLAYER_ID)).thenReturn(CompletableFuture.completedFuture(new HashMap<>()));

    instance.setPlayer(PlayerBuilder.create(PLAYER_NAME).id(PLAYER_ID).get());

    verify(achievementService).getAchievementDefinitions();
    verify(achievementService).getPlayerAchievements(PLAYER_ID);
    verify(eventService).getPlayerEvents(PLAYER_ID);

    assertThat(instance.mostRecentAchievementPane.isVisible(), is(true));
  }

  @Test
  public void testOnRatingTypeChangeGlobal() throws Exception {
    testSetPlayerInfoBean();
    instance.ratingTypeComboBox.setValue(instance.ratingTypeComboBox.getItems().get(0));
    instance.onRatingTypeChange();
    verify(statisticsService, times(2)).getRatingHistory(KnownFeaturedMod.FAF, PLAYER_ID);
  }

  @Test
  public void testOnRatingTypeChange1v1() throws Exception {
    testSetPlayerInfoBean();
    instance.ratingTypeComboBox.setValue(instance.ratingTypeComboBox.getItems().get(1));
    instance.onRatingTypeChange();
    verify(statisticsService).getRatingHistory(KnownFeaturedMod.LADDER_1V1, PLAYER_ID);
  }
}
