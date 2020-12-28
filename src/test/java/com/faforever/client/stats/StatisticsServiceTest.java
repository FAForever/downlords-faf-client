package com.faforever.client.stats;

import com.faforever.client.leaderboard.Leaderboard;
import com.faforever.client.leaderboard.LeaderboardBuilder;
import com.faforever.client.remote.FafService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class StatisticsServiceTest {

  @Mock
  private FafService fafService;

  private StatisticsService instance;
  private Leaderboard leaderboard;

  @Before
  public void setUp() throws Exception {
    leaderboard = LeaderboardBuilder.create().defaultValues().get();
    instance = new StatisticsService(fafService);
  }

  @Test
  public void testGetStatisticsForPlayer() throws Exception {
    instance.getRatingHistory(123, leaderboard);
    verify(fafService).getRatingHistory(123, leaderboard.getTechnicalName());
  }
}
