package com.faforever.client.stats;


import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.game.KnownFeaturedMod;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface StatisticsService {

  CompletableFuture<List<RatingHistoryDataPoint>> getRatingHistory(KnownFeaturedMod featuredMod, int playerId);
}
