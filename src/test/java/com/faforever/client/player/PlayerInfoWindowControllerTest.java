package com.faforever.client.player;

import com.faforever.client.achievements.AchievementItemController;
import com.faforever.client.achievements.AchievementService;
import com.faforever.client.builders.AchievementDefinitionBuilder;
import com.faforever.client.builders.LeaderboardBeanBuilder;
import com.faforever.client.builders.LeaderboardEntryBeanBuilder;
import com.faforever.client.builders.LeaderboardRatingBeanBuilder;
import com.faforever.client.builders.LeaderboardRatingJournalBeanBuilder;
import com.faforever.client.builders.LeaderboardRatingMapBuilder;
import com.faforever.client.builders.PlayerAchievementBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.stats.StatisticsService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.commons.api.dto.AchievementState;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerInfoWindowControllerTest extends UITest {

  private PlayerInfoWindowController instance;

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
  private PlayerRatingChartTooltipController playerRatingChartTooltipController;
  @Mock
  private TimeService timeService;
  @Mock
  private PlayerService playerService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private LeaderboardService leaderboardService;

  private LeaderboardBean leaderboard;
  private PlayerBean player;

  @BeforeEach
  public void setUp() throws Exception {
    leaderboard = LeaderboardBeanBuilder.create().defaultValues().get();
    player = PlayerBeanBuilder.create().defaultValues().username("junit").get();

    instance = new PlayerInfoWindowController(statisticsService, countryFlagService, achievementService, eventService,
        i18n, uiService, timeService, playerService, notificationService, leaderboardService);

    when(i18n.getOrDefault(leaderboard.getTechnicalName(), leaderboard.getNameKey())).thenReturn(leaderboard.getTechnicalName());
    when(i18n.get("leaderboard.rating", leaderboard.getTechnicalName())).thenReturn(leaderboard.getTechnicalName());
    when(i18n.number(anyInt())).thenReturn("123");
    when(uiService.loadFxml("theme/achievement_item.fxml")).thenReturn(achievementItemController);
    when(achievementItemController.getRoot()).thenReturn(new HBox());
    when(uiService.loadFxml("theme/chat/player_rating_chart_tooltip.fxml")).thenReturn(playerRatingChartTooltipController);
    when(playerRatingChartTooltipController.getRoot()).thenReturn(new Pane());
    when(playerService.getPlayersByIds(any())).thenReturn(CompletableFuture.completedFuture(List.of(player)));
    when(leaderboardService.getLeaderboards()).thenReturn(CompletableFuture.completedFuture(List.of(leaderboard)));
    when(leaderboardService.getEntriesForPlayer(eq(player))).thenReturn(CompletableFuture.completedFuture(List.of(LeaderboardEntryBeanBuilder.create().defaultValues().get())));
    when(statisticsService.getRatingHistory(eq(player), any())).thenReturn(CompletableFuture.completedFuture(asList(
        LeaderboardRatingJournalBeanBuilder.create().defaultValues().createTime(OffsetDateTime.now()).meanBefore(1500d).deviationBefore(50d).get(),
        LeaderboardRatingJournalBeanBuilder.create().defaultValues().createTime(OffsetDateTime.now().plusDays(1)).meanBefore(1500d).deviationBefore(50d).get()
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
    when(achievementService.getAchievementDefinitions()).thenReturn(CompletableFuture.completedFuture(List.of(
        AchievementDefinitionBuilder.create().id("foo-bar").get()
    )));
    when(uiService.loadFxml("theme/achievement_item.fxml")).thenReturn(achievementItemController);
    when(achievementService.getPlayerAchievements(player.getId())).thenReturn(CompletableFuture.completedFuture(List.of(
        PlayerAchievementBuilder.create().defaultValues().achievementId("foo-bar").state(AchievementState.UNLOCKED).get()
        )));
    when(eventService.getPlayerEvents(player.getId())).thenReturn(CompletableFuture.completedFuture(new HashMap<>()));
    player.setLeaderboardRatings(LeaderboardRatingMapBuilder.create().put(leaderboard.getTechnicalName(), LeaderboardRatingBeanBuilder.create().defaultValues().get()).get());

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
    verify(statisticsService, times(2)).getRatingHistory(player, leaderboard);
  }
}
