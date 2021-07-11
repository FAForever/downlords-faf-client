package com.faforever.client.stats;

import com.faforever.client.leaderboard.Leaderboard;
import com.faforever.client.leaderboard.LeaderboardBuilder;
import com.faforever.client.remote.FafService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class StatisticsServiceTest {

  @Mock
  private FafService fafService;

  private StatisticsService instance;
  private Leaderboard leaderboard;

  @BeforeEach
  public void setUp() throws Exception {
    leaderboard = LeaderboardBuilder.create().defaultValues().get();
    instance = new StatisticsService(fafService);
  }

  @Test
  public void testGetStatisticsForPlayer() throws Exception {
    instance.getRatingHistory(123, leaderboard);
    verify(fafService).getRatingHistory(123, leaderboard.getId());
  }
}
