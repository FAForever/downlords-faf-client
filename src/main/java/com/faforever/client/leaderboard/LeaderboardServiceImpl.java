package com.faforever.client.leaderboard;

import com.faforever.client.api.Ranked1v1Stats;
import com.faforever.client.remote.FafService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class LeaderboardServiceImpl implements LeaderboardService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  FafService fafService;

  @Override
  public CompletionStage<List<Ranked1v1EntryBean>> getRanked1v1Entries() {
    logger.debug("Fetching ranked 1v1 leaderboard from API");
    return fafService.getRanked1v1Entries();
  }

  @Override
  public CompletionStage<Ranked1v1Stats> getRanked1v1Stats() {
    logger.debug("Fetching ranked 1v1 stats from API");
    return fafService.getRanked1v1Stats();
  }

  @Override
  public CompletionStage<Ranked1v1EntryBean> getEntryForPlayer(int playerId) {
    logger.debug("Fetching ranked 1v1 entry for player: {}", playerId);
    return fafService.getRanked1v1EntryForPlayer(playerId);
  }
}
