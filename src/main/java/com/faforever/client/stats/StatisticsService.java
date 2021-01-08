package com.faforever.client.stats;

import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.leaderboard.Leaderboard;
import com.faforever.client.remote.FafService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;


@Lazy
@Service
@RequiredArgsConstructor
public class StatisticsService {

  private final FafService fafService;

  public CompletableFuture<List<RatingHistoryDataPoint>> getRatingHistory(int playerId, Leaderboard leaderboard) {
    return fafService.getRatingHistory(playerId, leaderboard.getTechnicalName());
  }
}
