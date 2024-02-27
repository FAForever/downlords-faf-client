package com.faforever.client.leaderboard;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.DivisionBean;
import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.LeagueSeasonBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.util.TimeService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class LeaderboardControllerTest extends PlatformTest {

  @InjectMocks
  private LeaderboardController instance;

  @Mock
  private I18n i18n;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private PlayerService playerService;
  @Mock
  private TimeService timeService;

  @Mock
  private LeaderboardRankingsController leaderboardRankingsController;
  @Mock
  private LeaderboardPlayerDetailsController leaderboardPlayerDetailsController;
  @Mock
  private LeaderboardDistributionController leaderboardDistributionController;

  private final ObjectProperty<LeagueSeasonBean> leagueSeasonProperty = new SimpleObjectProperty<>();

  private PlayerBean player;
  private final LeagueSeasonBean season = Instancio.of(LeagueSeasonBean.class)
                                                   .set(field(LeagueSeasonBean::nameKey), "seasonName")
                                                   .set(field(LeagueSeasonBean::seasonNumber), 1)
                                                   .create();
  ;
  private final DivisionBean division = Instancio.of(DivisionBean.class)
                                                 .set(field(DivisionBean::nameKey), "silver")
                                                 .set(field(DivisionBean::index), 2)
                                                 .create();

  private final SubdivisionBean subdivisionBean2 = Instancio.of(SubdivisionBean.class)
                                                            .set(field(SubdivisionBean::id), 2)
                                                            .set(field(SubdivisionBean::division), division)
                                                            .set(field(SubdivisionBean::index), 1)
                                                            .create();
  ;
  private final SubdivisionBean subdivisionBean1 = Instancio.of(SubdivisionBean.class)
                                                            .set(field(SubdivisionBean::id), 1)
                                                            .set(field(SubdivisionBean::index), 1)
                                                            .create();
  private final LeagueEntryBean leagueEntryBean2 = Instancio.of(LeagueEntryBean.class)
                                                            .set(field(LeagueEntryBean::subdivision), subdivisionBean1)
                                                            .set(field(LeagueEntryBean::id), 1)
                                                            .set(field(LeagueEntryBean::player),
                                                                 PlayerBeanBuilder.create()
                                                                                  .defaultValues()
                                                                                  .username("2")
                                                                                  .get())
                                                            .create();
  private final LeagueEntryBean leagueEntryBean1 = Instancio.of(LeagueEntryBean.class)
                                                            .set(field(LeagueEntryBean::subdivision), subdivisionBean1)
                                                            .set(field(LeagueEntryBean::id), 0)
                                                            .set(field(LeagueEntryBean::player),
                                                                 PlayerBeanBuilder.create()
                                                                                  .defaultValues()
                                                                                  .username("1")
                                                                                  .get())
                                                            .create();


  @BeforeEach
  public void setUp() throws Exception {
    player = PlayerBeanBuilder.create().defaultValues().id(3).username("junit").get();
    lenient().when(i18n.getOrDefault("seasonName", "leagueLeaderboard.season.seasonName", 1))
             .thenReturn("seasonName 1");
    lenient().when(i18n.get("leaderboard.seasonDate", null, null)).thenReturn("-");

    lenient().when(leaderboardService.getAllSubdivisions(season))
             .thenReturn(Flux.just(subdivisionBean1, subdivisionBean2));
    lenient().when(leaderboardService.getActiveEntries(season))
             .thenReturn(Flux.just(leagueEntryBean1, leagueEntryBean2));
    lenient().when(leaderboardService.getLeagueEntryForPlayer(player, season)).thenReturn(Mono.empty());
    lenient().when(playerService.getCurrentPlayer()).thenReturn(player);

    when(leaderboardPlayerDetailsController.leagueSeasonProperty()).thenReturn(leagueSeasonProperty);

    loadFxml("theme/leaderboard/leaderboard.fxml", clazz -> {
      if (clazz == LeaderboardRankingsController.class) {
        return leaderboardRankingsController;
      }

      if (clazz == LeaderboardPlayerDetailsController.class) {
        return leaderboardPlayerDetailsController;
      }

      if (clazz == LeaderboardDistributionController.class) {
        return leaderboardDistributionController;
      }

      return instance;
    });
  }

  @Test
  public void testSetSeason() {
    runOnFxThreadAndWait(() -> instance.setLeagueSeason(season));

    assertEquals("SEASONNAME 1", instance.seasonLabel.getText());
    verifyNoInteractions(notificationService);

    assertEquals(season, leagueSeasonProperty.get());
    verify(leaderboardDistributionController).setSubdivisions(List.of(subdivisionBean1, subdivisionBean2));
    verify(leaderboardRankingsController).setSubdivisions(List.of(subdivisionBean1, subdivisionBean2));
    verify(leaderboardDistributionController).setLeagueEntries(List.of(leagueEntryBean1, leagueEntryBean2));
    verify(leaderboardRankingsController).setLeagueEntries(List.of(leagueEntryBean1, leagueEntryBean2));
    verify(leaderboardPlayerDetailsController, times(2)).setLeagueEntry(null);
  }

  @Test
  public void testInitializeWithSeasonError() {
    when(leaderboardService.getAllSubdivisions(season)).thenReturn(Flux.error(new FakeTestException()));
    when(leaderboardService.getActiveEntries(season)).thenReturn(Flux.error(new FakeTestException()));
    when(leaderboardService.getLeagueEntryForPlayer(player, season)).thenReturn(Mono.error(new FakeTestException()));

    runOnFxThreadAndWait(() -> instance.setLeagueSeason(season));

    verify(notificationService).addImmediateErrorNotification(any(), eq("leaderboard.failedToLoadEntry"));
    verify(notificationService).addImmediateErrorNotification(any(), eq("leaderboard.failedToLoadEntries"));
    verify(notificationService).addImmediateErrorNotification(any(), eq("leaderboard.failedToLoadDivisions"));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.getRoot(), instance.leaderboardRoot);
  }
}
