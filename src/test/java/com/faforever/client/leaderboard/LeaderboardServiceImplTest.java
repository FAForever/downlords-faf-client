package com.faforever.client.leaderboard;


import com.faforever.client.remote.FafService;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LeaderboardServiceImplTest extends ServiceTest {

  private static final int PLAYER_ID = 123;
  @Mock
  private FafService fafService;

  private LeaderboardServiceImpl instance;

  private Leaderboard leaderboard;

  @BeforeEach
  public void setUp() throws Exception {
    leaderboard = LeaderboardBuilder.create().defaultValues().get();

    instance = new LeaderboardServiceImpl(fafService);
  }

  @Test
  public void testGetLeaderboardEntries() {
    List<LeaderboardEntry> ladder1V1Entries = Collections.emptyList();
    when(fafService.getAllLeaderboardEntries(leaderboard.getTechnicalName())).thenReturn(CompletableFuture.completedFuture(ladder1V1Entries));

    List<LeaderboardEntry> result = instance.getEntries(leaderboard).toCompletableFuture().join();

    verify(fafService).getAllLeaderboardEntries(leaderboard.getTechnicalName());
    assertThat(result, is(ladder1V1Entries));
  }

  @Test
  public void testGetLeaderboardStats() {
    LeaderboardEntry leaderboardEntry1 = new LeaderboardEntry();
    leaderboardEntry1.setRating(151);
    leaderboardEntry1.setGamesPlayed(LeaderboardService.MINIMUM_GAMES_PLAYED_TO_BE_SHOWN);

    LeaderboardEntry leaderboardEntry2 = new LeaderboardEntry();
    leaderboardEntry2.setRating(121);
    leaderboardEntry2.setGamesPlayed(LeaderboardService.MINIMUM_GAMES_PLAYED_TO_BE_SHOWN + 42);

    LeaderboardEntry leaderboardEntry3 = new LeaderboardEntry();
    leaderboardEntry3.setRating(221);
    leaderboardEntry3.setGamesPlayed(LeaderboardService.MINIMUM_GAMES_PLAYED_TO_BE_SHOWN);

    when(fafService.getAllLeaderboardEntries(leaderboard.getTechnicalName())).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        leaderboardEntry1, leaderboardEntry2, leaderboardEntry3
    )));

    List<RatingStat> result = instance.getLeaderboardStats(leaderboard.getTechnicalName()).join();
    verify(fafService).getAllLeaderboardEntries(leaderboard.getTechnicalName());

    result.sort(Comparator.comparingInt(RatingStat::getRating));

    assertEquals(2, result.size());
    assertEquals(2, result.get(0).getTotalCount());
    assertEquals(2, result.get(0).getCountWithEnoughGamesPlayed());
    assertEquals(100, result.get(0).getRating());

    assertEquals(1, result.get(1).getTotalCount());
    assertEquals(1, result.get(1).getCountWithEnoughGamesPlayed());
    assertEquals(200, result.get(1).getRating());
  }

  @Test
  public void testStatsOnlyShowsPlayersWithEnoughGamesPlayed() throws Exception {
    LeaderboardEntry leaderboardEntry1 = new LeaderboardEntry();
    leaderboardEntry1.setRating(151);
    leaderboardEntry1.setGamesPlayed(LeaderboardService.MINIMUM_GAMES_PLAYED_TO_BE_SHOWN);

    LeaderboardEntry leaderboardEntry2 = new LeaderboardEntry();
    leaderboardEntry2.setRating(121);
    leaderboardEntry2.setGamesPlayed(LeaderboardService.MINIMUM_GAMES_PLAYED_TO_BE_SHOWN - 1);

    LeaderboardEntry leaderboardEntry3 = new LeaderboardEntry();
    leaderboardEntry3.setRating(221);
    leaderboardEntry3.setGamesPlayed(LeaderboardService.MINIMUM_GAMES_PLAYED_TO_BE_SHOWN - 1);

    when(fafService.getAllLeaderboardEntries(leaderboard.getTechnicalName())).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        leaderboardEntry1, leaderboardEntry2, leaderboardEntry3
    )));

    List<RatingStat> result = instance.getLeaderboardStats(leaderboard.getTechnicalName()).toCompletableFuture().get(2, TimeUnit.SECONDS);
    verify(fafService).getAllLeaderboardEntries(leaderboard.getTechnicalName());

    assertEquals(2, result.size());
    assertEquals(2, result.get(0).getTotalCount());
    assertEquals(1, result.get(0).getCountWithEnoughGamesPlayed());
    assertEquals(100, result.get(0).getRating());

    assertEquals(1, result.get(1).getTotalCount());
    assertEquals(0, result.get(1).getCountWithEnoughGamesPlayed());
    assertEquals(200, result.get(1).getRating());
  }

  @Test
  public void testGetEntriesForPlayer() {
    LeaderboardEntry entry = new LeaderboardEntry();
    when(fafService.getLeaderboardEntriesForPlayer(PLAYER_ID)).thenReturn(CompletableFuture.completedFuture(List.of(entry)));

    List<LeaderboardEntry> result = instance.getEntriesForPlayer(PLAYER_ID).toCompletableFuture().join();
    verify(fafService).getLeaderboardEntriesForPlayer(PLAYER_ID);
    assertEquals(List.of(entry), result);
  }
}
