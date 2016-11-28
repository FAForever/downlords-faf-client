package com.faforever.client.leaderboard;

import com.faforever.client.api.Ranked1v1Stats;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.remote.FafService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LeaderboardServiceImplTest {

  private static final int PLAYER_ID = 123;
  @Mock
  private FafService fafService;

  private LeaderboardServiceImpl instance;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new LeaderboardServiceImpl();
    instance.fafService = fafService;
  }

  @Test
  public void testGetLeaderboardEntries() throws Exception {
    List<Ranked1v1EntryBean> ranked1v1EntryBeans = Collections.emptyList();
    when(fafService.getLeaderboardEntries(any())).thenReturn(CompletableFuture.completedFuture(ranked1v1EntryBeans));

    List<Ranked1v1EntryBean> result = instance.getEntries(KnownFeaturedMod.LADDER_1V1).toCompletableFuture().get(2, TimeUnit.SECONDS);

    verify(fafService).getLeaderboardEntries(KnownFeaturedMod.LADDER_1V1);
    assertThat(result, is(ranked1v1EntryBeans));
  }

  @Test
  public void testGetRanked1v1Stats() throws Exception {
    Ranked1v1Stats ranked1v1Stats = new Ranked1v1Stats();
    when(fafService.getRanked1v1Stats()).thenReturn(CompletableFuture.completedFuture(ranked1v1Stats));

    Ranked1v1Stats result = instance.getRanked1v1Stats().toCompletableFuture().get(2, TimeUnit.SECONDS);
    verify(fafService).getRanked1v1Stats();
    assertThat(result, is(ranked1v1Stats));
  }

  @Test
  public void testGetEntryForPlayer() throws Exception {
    Ranked1v1EntryBean entry = new Ranked1v1EntryBean();
    when(fafService.getRanked1v1EntryForPlayer(PLAYER_ID)).thenReturn(CompletableFuture.completedFuture(entry));

    Ranked1v1EntryBean result = instance.getEntryForPlayer(PLAYER_ID).toCompletableFuture().get(2, TimeUnit.SECONDS);
    verify(fafService).getRanked1v1EntryForPlayer(PLAYER_ID);
    assertThat(result, is(entry));
  }
}
