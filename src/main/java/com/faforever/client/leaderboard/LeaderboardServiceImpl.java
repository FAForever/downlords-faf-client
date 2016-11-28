package com.faforever.client.leaderboard;

import com.faforever.client.api.Ranked1v1Stats;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.remote.FafService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletionStage;


@Lazy
@Service
@Profile("!local")
public class LeaderboardServiceImpl implements LeaderboardService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Inject
  FafService fafService;

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

  @Override
  public CompletionStage<List<Ranked1v1EntryBean>> getEntries(KnownFeaturedMod ratingType) {
    return fafService.getLeaderboardEntries(ratingType);
  }
}
