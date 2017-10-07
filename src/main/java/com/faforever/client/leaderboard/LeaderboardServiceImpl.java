package com.faforever.client.leaderboard;

import com.faforever.client.FafClientApplication;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.remote.FafService;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Lazy
@Service
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
public class LeaderboardServiceImpl implements LeaderboardService {

  private final FafService fafService;

  @Inject
  public LeaderboardServiceImpl(FafService fafService) {
    this.fafService = fafService;
  }

  @Override
  public CompletableFuture<List<RatingStat>> getLadder1v1Stats() {
    return fafService.getLadder1v1Leaderboard()
        .thenApply(entries -> entries.stream()
            .collect(Collectors.groupingBy(leaderboardEntry -> (int) leaderboardEntry.getRating() / 100 * 100, Collectors.counting()))
            .entrySet().stream()
            .map(entry -> new RatingStat(entry.getKey(), entry.getValue().intValue()))
            .collect(Collectors.toList()));
  }

  @Override
  public CompletableFuture<LeaderboardEntry> getEntryForPlayer(int playerId) {
    return fafService.getLadder1v1EntryForPlayer(playerId);
  }

  @Override
  public CompletableFuture<List<LeaderboardEntry>> getEntries(KnownFeaturedMod ratingType) {
    switch (ratingType) {
      case FAF:
        return fafService.getGlobalLeaderboard();
      case LADDER_1V1:
        return fafService.getLadder1v1Leaderboard();
      default:
        throw new IllegalArgumentException("Not supported: " + ratingType);
    }
  }
}
