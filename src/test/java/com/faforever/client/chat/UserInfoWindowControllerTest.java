package com.faforever.client.chat;

import com.faforever.client.achievements.AchievementDefinitionBuilder;
import com.faforever.client.achievements.AchievementItemController;
import com.faforever.client.achievements.AchievementService;
import com.faforever.client.achievements.PlayerAchievementBuilder;
import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.events.EventService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.Leaderboard;
import com.faforever.client.leaderboard.LeaderboardBuilder;
import com.faforever.client.leaderboard.LeaderboardEntryBuilder;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.stats.StatisticsService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.commons.api.dto.AchievementState;
import javafx.scene.layout.HBox;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserInfoWindowControllerTest extends AbstractPlainJavaFxTest {

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

  private Leaderboard leaderboard;
  private Player player;

  @Before
  public void setUp() throws Exception {
    leaderboard = LeaderboardBuilder.create().defaultValues().get();
    player = PlayerBuilder.create("junit").defaultValues().get();

    instance = new UserInfoWindowController(statisticsService, countryFlagService, achievementService, eventService,
        i18n, uiService, timeService, playerService, notificationService, leaderboardService);

    when(i18n.getWithDefault(leaderboard.getTechnicalName(), leaderboard.getNameKey())).thenReturn(leaderboard.getTechnicalName());
    when(i18n.get("leaderboard.rating", leaderboard.getTechnicalName())).thenReturn(leaderboard.getTechnicalName());
    when(i18n.number(anyInt())).thenReturn("123");
    when(uiService.loadFxml("theme/achievement_item.fxml")).thenReturn(achievementItemController);
    when(achievementItemController.getRoot()).thenReturn(new HBox());
    when(playerService.getPlayersByIds(any())).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    when(leaderboardService.getLeaderboards()).thenReturn(CompletableFuture.completedFuture(List.of(leaderboard)));
    when(leaderboardService.getEntriesForPlayer(eq(player.getId()))).thenReturn(CompletableFuture.completedFuture(List.of(LeaderboardEntryBuilder.create().defaultValues().get())));
    when(statisticsService.getRatingHistory(eq(player.getId()), any())).thenReturn(CompletableFuture.completedFuture(asList(
        new RatingHistoryDataPoint(OffsetDateTime.now(), 1500f, 50f),
        new RatingHistoryDataPoint(OffsetDateTime.now().plus(1, ChronoUnit.DAYS), 1500f, 50f)
    )));

    loadFxml("theme/user_info_window.fxml", clazz -> instance);
  }

  @Test
  public void testSetPlayerInfoBeanNoAchievementUnlocked() {
    when(achievementService.getAchievementDefinitions()).thenReturn(CompletableFuture.completedFuture(singletonList(
        AchievementDefinitionBuilder.create().defaultValues().get()
    )));
    when(uiService.loadFxml("theme/achievement_item.fxml")).thenReturn(achievementItemController);
    when(achievementService.getPlayerAchievements(player.getId())).thenReturn(CompletableFuture.completedFuture(
        singletonList(PlayerAchievementBuilder.create().defaultValues().get())
    ));
    when(eventService.getPlayerEvents(player.getId())).thenReturn(CompletableFuture.completedFuture(new HashMap<>()));

    instance.setPlayer(player);

    verify(achievementService).getAchievementDefinitions();
    verify(achievementService).getPlayerAchievements(player.getId());
    verify(eventService).getPlayerEvents(player.getId());

    assertFalse(instance.mostRecentAchievementPane.isVisible());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.userInfoRoot, instance.getRoot());
    assertNull(instance.getRoot().getParent());
  }

  @Test
  public void testSetPlayerInfoBean() {
    when(achievementService.getAchievementDefinitions()).thenReturn(CompletableFuture.completedFuture(asList(
        AchievementDefinitionBuilder.create().id("foo-bar").get(),
        AchievementDefinitionBuilder.create().defaultValues().get()
    )));
    when(uiService.loadFxml("theme/achievement_item.fxml")).thenReturn(achievementItemController);
    when(achievementService.getPlayerAchievements(player.getId())).thenReturn(CompletableFuture.completedFuture(asList(
        PlayerAchievementBuilder.create().defaultValues().achievementId("foo-bar").state(AchievementState.UNLOCKED).get(),
        PlayerAchievementBuilder.create().defaultValues().get()
    )));
    when(eventService.getPlayerEvents(player.getId())).thenReturn(CompletableFuture.completedFuture(new HashMap<>()));

    instance.setPlayer(player);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.ratingsLabels.getText().contains(leaderboard.getTechnicalName()));
    assertTrue(instance.ratingsValues.getText().contains("123"));
    verify(achievementService).getAchievementDefinitions();
    verify(achievementService).getPlayerAchievements(player.getId());
    verify(leaderboardService, times(2)).getLeaderboards();
    verify(eventService).getPlayerEvents(player.getId());

    assertTrue(instance.mostRecentAchievementPane.isVisible());
  }

  @Test
  public void testOnRatingTypeChange() {
    testSetPlayerInfoBean();
    instance.ratingTypeComboBox.setValue(leaderboard);
    instance.onRatingTypeChange();
    verify(statisticsService, times(2)).getRatingHistory(player.getId(), leaderboard);
  }
}
