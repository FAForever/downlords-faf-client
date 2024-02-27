package com.faforever.client.player;

import com.faforever.client.achievements.AchievementItemController;
import com.faforever.client.achievements.AchievementService;
import com.faforever.client.builders.AchievementDefinitionBuilder;
import com.faforever.client.builders.LeaderboardEntryBeanBuilder;
import com.faforever.client.builders.LeaderboardRatingBeanBuilder;
import com.faforever.client.builders.LeaderboardRatingJournalBeanBuilder;
import com.faforever.client.builders.LeaderboardRatingMapBuilder;
import com.faforever.client.builders.LeagueSeasonBeanBuilder;
import com.faforever.client.builders.PlayerAchievementBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeaderboardRatingBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.stats.StatisticsService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.commons.api.dto.AchievementState;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

public class PlayerInfoWindowControllerTest extends PlatformTest {

  @InjectMocks
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
  private UserLeaderboardInfoController userLeaderboardInfoController;
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
    leaderboard = Instancio.create(LeaderboardBean.class);
    player = PlayerBeanBuilder.create().defaultValues().username("junit").get();

    lenient().when(i18n.getOrDefault(leaderboard.technicalName(), leaderboard.nameKey()))
             .thenReturn(leaderboard.technicalName());
    lenient().when(uiService.loadFxml("theme/achievement_item.fxml")).thenReturn(achievementItemController);
    lenient().when(achievementItemController.getRoot()).thenReturn(new HBox());
    lenient().when(uiService.loadFxml("theme/chat/player_rating_chart_tooltip.fxml"))
             .thenReturn(playerRatingChartTooltipController);
    lenient().when(playerRatingChartTooltipController.getRoot()).thenReturn(new Pane());
    lenient().when(playerService.getPlayerNames(any())).thenReturn(Flux.empty());
    lenient().when(leaderboardService.getLeaderboards()).thenReturn(Flux.just(leaderboard));
    lenient().when(leaderboardService.getEntriesForPlayer(eq(player)))
             .thenReturn(Flux.just(LeaderboardEntryBeanBuilder.create().defaultValues().get()));
    lenient().when(statisticsService.getRatingHistory(eq(player), any())).thenReturn(Flux.just(
        LeaderboardRatingJournalBeanBuilder.create().defaultValues().createTime(OffsetDateTime.now()).meanBefore(1500d).deviationBefore(50d).get(),
        LeaderboardRatingJournalBeanBuilder.create().defaultValues().createTime(OffsetDateTime.now().plusDays(1)).meanBefore(1500d).deviationBefore(50d).get()));

    loadFxml("theme/user_info_window.fxml", clazz -> instance);
  }

  @Test
  public void testSetPlayerInfoBeanNoAchievementUnlocked() {
    when(achievementService.getAchievementDefinitions()).thenReturn(
        Flux.just(AchievementDefinitionBuilder.create().defaultValues().get()));
    when(uiService.loadFxml("theme/achievement_item.fxml")).thenReturn(achievementItemController);
    when(achievementService.getPlayerAchievements(player.getId())).thenReturn(
        Flux.just(PlayerAchievementBuilder.create().defaultValues().get()));
    when(eventService.getPlayerEvents(player.getId())).thenReturn(Flux.empty());

    instance.setPlayer(player);
    waitForFxEvents();

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
    when(achievementService.getAchievementDefinitions()).thenReturn(
        Flux.just(AchievementDefinitionBuilder.create().id("foo-bar").get()));
    when(uiService.loadFxml("theme/achievement_item.fxml")).thenReturn(achievementItemController);
    when(uiService.loadFxml("theme/user_leaderboard_info.fxml")).thenReturn(userLeaderboardInfoController);
    when(achievementService.getPlayerAchievements(player.getId())).thenReturn(Flux.just(
        PlayerAchievementBuilder.create()
                                .defaultValues()
                                .achievementId("foo-bar")
                                .state(AchievementState.UNLOCKED)
                                .get()));
    when(eventService.getPlayerEvents(player.getId())).thenReturn(Flux.empty());
    when(leaderboardService.getActiveSeasons()).thenReturn(
        Flux.just(LeagueSeasonBeanBuilder.create().defaultValues().leaderboard(leaderboard).get()));
    when(leaderboardService.getActiveLeagueEntryForPlayer(player, leaderboard.technicalName())).thenReturn(
        Mono.empty());
    when(userLeaderboardInfoController.getRoot()).thenReturn(new VBox());
    final LeaderboardRatingBean leaderboardRating = LeaderboardRatingBeanBuilder.create()
        .defaultValues()
        .numberOfGames(47)
        .mean(500)
        .deviation(100)
        .get();
    player.setLeaderboardRatings(
        LeaderboardRatingMapBuilder.create().put(leaderboard.technicalName(), leaderboardRating).get());

    instance.setPlayer(player);
    WaitForAsyncUtils.waitForFxEvents();

    verify(achievementService).getAchievementDefinitions();
    verify(achievementService).getPlayerAchievements(player.getId());
    verify(leaderboardService, times(2)).getLeaderboards();
    verify(eventService).getPlayerEvents(player.getId());
    verify(userLeaderboardInfoController).setLeaderboardInfo(player, leaderboard);
    verify(userLeaderboardInfoController).getRoot();
    assertEquals(1, instance.leaderboardBox.getChildren().size());
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
