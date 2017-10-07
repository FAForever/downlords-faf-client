package com.faforever.client.leaderboard;


import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.remote.FafService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LeaderboardServiceImplTest {

  private static final int PLAYER_ID = 123;
  @Mock
  private FafService fafService;

  private LeaderboardServiceImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new LeaderboardServiceImpl(fafService);
  }

  @Test
  public void testGetLeaderboardEntries() throws Exception {
    List<LeaderboardEntry> ladder1V1Entries = Collections.emptyList();
    when(fafService.getLadder1v1Leaderboard()).thenReturn(CompletableFuture.completedFuture(ladder1V1Entries));

    List<LeaderboardEntry> result = instance.getEntries(KnownFeaturedMod.LADDER_1V1).toCompletableFuture().get(2, TimeUnit.SECONDS);

    verify(fafService).getLadder1v1Leaderboard();
    assertThat(result, is(ladder1V1Entries));
  }

  @Test
  public void testRoundRatings() {
    assertThat(instance.roundRatingToLowerHundred(99.99), is(0));
    assertThat(instance.roundRatingToLowerHundred(0.0), is(0));
    assertThat(instance.roundRatingToLowerHundred(42.0), is(0));


    assertThat(instance.roundRatingToLowerHundred(-99.99), is(-100));
    assertThat(instance.roundRatingToLowerHundred(-1), is(-100));
    assertThat(instance.roundRatingToLowerHundred(-100.1), is(-200));

    assertThat(instance.roundRatingToLowerHundred(199.99), is(100));
    assertThat(instance.roundRatingToLowerHundred(242), is(200));
    assertThat(instance.roundRatingToLowerHundred(2000.1), is(2000));
    assertThat(instance.roundRatingToLowerHundred(2099.9), is(2000));
  }

  @Test
  public void testGetLadder1v1Stats() throws Exception {
    LeaderboardEntry leaderboardEntry1 = new LeaderboardEntry();
    leaderboardEntry1.setRating(151);
    leaderboardEntry1.setGamesPlayed(LeaderboardService.MINIMUM_GAMES_PLAYED_TO_BE_SHOWN);

    LeaderboardEntry leaderboardEntry2 = new LeaderboardEntry();
    leaderboardEntry2.setRating(121);
    leaderboardEntry2.setGamesPlayed(LeaderboardService.MINIMUM_GAMES_PLAYED_TO_BE_SHOWN + 42);

    LeaderboardEntry leaderboardEntry3 = new LeaderboardEntry();
    leaderboardEntry3.setRating(221);
    leaderboardEntry3.setGamesPlayed(LeaderboardService.MINIMUM_GAMES_PLAYED_TO_BE_SHOWN);

    when(fafService.getLadder1v1Leaderboard()).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        leaderboardEntry1, leaderboardEntry2, leaderboardEntry3
    )));

    List<RatingStat> result = instance.getLadder1v1Stats().toCompletableFuture().get(2, TimeUnit.SECONDS);
    verify(fafService).getLadder1v1Leaderboard();

    result.sort(Comparator.comparingInt(RatingStat::getRating));

    assertThat(result, hasSize(2));
    assertThat(result.get(0).getTotalCount(), is(2));
    assertThat(result.get(0).getCountWithEnoughGamesPlayed(), is(2));
    assertThat(result.get(0).getRating(), is(100));

    assertThat(result.get(1).getTotalCount(), is(1));
    assertThat(result.get(1).getCountWithEnoughGamesPlayed(), is(1));
    assertThat(result.get(1).getRating(), is(200));
  }

  @Test
  public void  testStatsOnlyShowsPlayersWithEnoughGamesPlayed() throws Exception {
    LeaderboardEntry leaderboardEntry1 = new LeaderboardEntry();
    leaderboardEntry1.setRating(151);
    leaderboardEntry1.setGamesPlayed(LeaderboardService.MINIMUM_GAMES_PLAYED_TO_BE_SHOWN);

    LeaderboardEntry leaderboardEntry2 = new LeaderboardEntry();
    leaderboardEntry2.setRating(121);
    leaderboardEntry2.setGamesPlayed(LeaderboardService.MINIMUM_GAMES_PLAYED_TO_BE_SHOWN -1);

    LeaderboardEntry leaderboardEntry3 = new LeaderboardEntry();
    leaderboardEntry3.setRating(221);
    leaderboardEntry3.setGamesPlayed(LeaderboardService.MINIMUM_GAMES_PLAYED_TO_BE_SHOWN -1);

    when(fafService.getLadder1v1Leaderboard()).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        leaderboardEntry1, leaderboardEntry2, leaderboardEntry3
    )));

    List<RatingStat> result = instance.getLadder1v1Stats().toCompletableFuture().get(2, TimeUnit.SECONDS);
    verify(fafService).getLadder1v1Leaderboard();

    assertThat(result, hasSize(2));
    assertThat(result.get(0).getTotalCount(), is(2));
    assertThat(result.get(0).getCountWithEnoughGamesPlayed(), is(1));
    assertThat(result.get(0).getRating(), is(100));

    assertThat(result.get(1).getTotalCount(), is(1));
    assertThat(result.get(1).getCountWithEnoughGamesPlayed(), is(0));
    assertThat(result.get(1).getRating(), is(200));
  }

  @Test
  public void testGetEntryForPlayer() throws Exception {
    LeaderboardEntry entry = new LeaderboardEntry();
    when(fafService.getLadder1v1EntryForPlayer(PLAYER_ID)).thenReturn(CompletableFuture.completedFuture(entry));

    LeaderboardEntry result = instance.getEntryForPlayer(PLAYER_ID).toCompletableFuture().get(2, TimeUnit.SECONDS);
    verify(fafService).getLadder1v1EntryForPlayer(PLAYER_ID);
    assertThat(result, is(entry));
  }
}
