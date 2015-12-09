package com.faforever.client.leaderboard;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.Ranked1v1Stats;
import com.faforever.client.config.CacheNames;
import org.springframework.cache.annotation.Cacheable;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class LeaderboardServiceImpl implements LeaderboardService {

  @Resource
  FafApiAccessor fafApiAccessor;
  @Resource
  Executor executor;

  @Override
  @Cacheable(CacheNames.LEADERBOARD)
  public CompletableFuture<List<Ranked1v1EntryBean>> getLeaderboardEntries() {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getRanked1v1Entries(), executor);
  }

  @Override
  public CompletableFuture<Ranked1v1Stats> getRanked1v1Stats() {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getRanked1v1Stats(), executor);
  }

  @Override
  public CompletableFuture<Ranked1v1EntryBean> getEntryForPlayer(int playerId) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getRanked1v1EntryForPlayer(playerId), executor);
  }
}
