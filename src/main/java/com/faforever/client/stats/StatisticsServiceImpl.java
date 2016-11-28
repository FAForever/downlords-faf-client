package com.faforever.client.stats;

import com.faforever.client.api.RatingType;
import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.remote.FafService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Lazy
@Service
public class StatisticsServiceImpl implements StatisticsService {

  @Inject
  FafService fafService;

  @Override
  public CompletableFuture<List<RatingHistoryDataPoint>> getRatingHistory(RatingType type, int playerId) {
    return fafService.getRatingHistory(type, playerId);
  }
}
