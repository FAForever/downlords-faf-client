package com.faforever.client.stats;


import com.faforever.client.api.RatingType;
import com.faforever.client.domain.RatingHistoryDataPoint;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface StatisticsService {

  CompletableFuture<List<RatingHistoryDataPoint>> getRatingHistory(RatingType type, int playerId);
}
