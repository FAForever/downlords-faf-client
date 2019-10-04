package com.faforever.client.stats;

import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.remote.FafService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Lazy
@Service
public class StatisticsService {

  private final FafService fafService;


  public StatisticsService(FafService fafService) {
    this.fafService = fafService;
  }

  public CompletableFuture<List<RatingHistoryDataPoint>> getRatingHistory(KnownFeaturedMod featuredMod, int playerId) {
    return fafService.getRatingHistory(playerId, featuredMod);
  }
}
