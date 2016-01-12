package com.faforever.client.leaderboard;

import com.faforever.client.api.FafApiAccessor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LeaderboardServiceImplTest {

  private static final int PLAYER_ID = 123;
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private Executor executor;

  private LeaderboardServiceImpl instance;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new LeaderboardServiceImpl();
    instance.fafApiAccessor = fafApiAccessor;
    instance.executor = executor;

    doAnswer(invocation -> {
      invocation.getArgumentAt(0, Runnable.class).run();
      return null;
    }).when(executor).execute(any());
  }

  @Test
  public void testGetLadderInfo() throws Exception {
    List<Ranked1v1EntryBean> ranked1v1EntryBeans = Collections.emptyList();
    when(fafApiAccessor.getRanked1v1Entries()).thenReturn(ranked1v1EntryBeans);

    CompletableFuture<List<Ranked1v1EntryBean>> future = instance.getLeaderboardEntries();

    verify(fafApiAccessor).getRanked1v1Entries();
    assertThat(future.get(3, TimeUnit.SECONDS), is(ranked1v1EntryBeans));
  }

  @Test
  public void testGetLeaderboardEntries() throws Exception {
    instance.getLeaderboardEntries().get(2, TimeUnit.SECONDS);
    verify(fafApiAccessor).getRanked1v1Entries();
  }

  @Test
  public void testGetRanked1v1Stats() throws Exception {
    instance.getRanked1v1Stats().get(2, TimeUnit.SECONDS);
    verify(fafApiAccessor).getRanked1v1Stats();
  }

  @Test
  public void testGetEntryForPlayer() throws Exception {
    instance.getEntryForPlayer(PLAYER_ID);
    verify(fafApiAccessor).getRanked1v1EntryForPlayer(PLAYER_ID);
  }
}
