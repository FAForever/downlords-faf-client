package com.faforever.client.stats;

import com.faforever.client.api.RatingType;
import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.remote.FafService;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StatisticsServiceImpl implements StatisticsService {

  @Resource
  FafService fafService;

  @Override
  public CompletableFuture<List<RatingHistoryDataPoint>> getRatingHistory(RatingType type, int playerId) {
    return fafService.getRatingHistory(type, playerId);
  }
}
