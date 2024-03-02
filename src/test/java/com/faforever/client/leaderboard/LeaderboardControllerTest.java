package com.faforever.client.leaderboard;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.api.Division;
import com.faforever.client.domain.api.LeagueEntry;
import com.faforever.client.domain.api.LeagueSeason;
import com.faforever.client.domain.api.Subdivision;
import com.faforever.client.domain.server.PlayerInfo;
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

  private final ObjectProperty<LeagueSeason> leagueSeasonProperty = new SimpleObjectProperty<>();

  private PlayerInfo player;
  private final LeagueSeason season = Instancio.of(LeagueSeason.class)
                                               .set(field(LeagueSeason::nameKey), "seasonName")
                                               .set(field(LeagueSeason::seasonNumber), 1)
                                               .create();
  ;
  private final Division division = Instancio.of(Division.class)
                                             .set(field(Division::nameKey), "silver")
                                             .set(field(Division::index), 2)
                                             .create();

  private final Subdivision subdivision2 = Instancio.of(Subdivision.class)
                                                    .set(field(Subdivision::id), 2)
                                                    .set(field(Subdivision::division), division)
                                                    .set(field(Subdivision::index), 1)
                                                    .create();
  ;
  private final Subdivision subdivision1 = Instancio.of(Subdivision.class)
                                                    .set(field(Subdivision::id), 1)
                                                    .set(field(Subdivision::index), 1)
                                                    .create();
  private final LeagueEntry leagueEntry2 = Instancio.of(LeagueEntry.class)
                                                    .set(field(LeagueEntry::subdivision), subdivision1)
                                                    .set(field(LeagueEntry::id), 1)
                                                    .set(field(LeagueEntry::player),
                                                         PlayerInfoBuilder.create().defaultValues().username("2").get())
                                                    .create();
  private final LeagueEntry leagueEntry1 = Instancio.of(LeagueEntry.class)
                                                    .set(field(LeagueEntry::subdivision), subdivision1)
                                                    .set(field(LeagueEntry::id), 0)
                                                    .set(field(LeagueEntry::player),
                                                         PlayerInfoBuilder.create().defaultValues().username("1").get())
                                                    .create();


  @BeforeEach
  public void setUp() throws Exception {
    player = PlayerInfoBuilder.create().defaultValues().id(3).username("junit").get();
    lenient().when(i18n.getOrDefault("seasonName", "leagueLeaderboard.season.seasonName", 1))
             .thenReturn("seasonName 1");
    lenient().when(i18n.get("leaderboard.seasonDate", null, null)).thenReturn("-");

    lenient().when(leaderboardService.getAllSubdivisions(season)).thenReturn(Flux.just(subdivision1, subdivision2));
    lenient().when(leaderboardService.getActiveEntries(season)).thenReturn(Flux.just(leagueEntry1, leagueEntry2));
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
    verify(leaderboardDistributionController).setSubdivisions(List.of(subdivision1, subdivision2));
    verify(leaderboardRankingsController).setSubdivisions(List.of(subdivision1, subdivision2));
    verify(leaderboardDistributionController).setLeagueEntries(List.of(leagueEntry1, leagueEntry2));
    verify(leaderboardRankingsController).setLeagueEntries(List.of(leagueEntry1, leagueEntry2));
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
