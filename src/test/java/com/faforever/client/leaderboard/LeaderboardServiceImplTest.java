package com.faforever.client.leaderboard;

import com.faforever.client.remote.FafService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LeaderboardServiceImplTest {

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
  public void testGetLadderInfo() throws Exception {
    List<LeaderboardEntryBean> entries = Collections.emptyList();
    when(fafService.requestLeaderboardEntries()).thenReturn(CompletableFuture.completedFuture(entries));

    CompletableFuture<List<LeaderboardEntryBean>> future = instance.getLeaderboardEntries();

    verify(fafService).requestLeaderboardEntries();
    assertThat(future, is(notNullValue()));
  }
}
