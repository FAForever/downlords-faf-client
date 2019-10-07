package com.faforever.client.leaderboard;

import com.faforever.client.FafClientApplication;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.remote.FafService;
import com.faforever.client.util.RatingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Lazy
@Service
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {
  private final FafService fafService;

  @Override
  public CompletableFuture<List<RatingStat>> getLadder1v1Stats() {
    return fafService.getLadder1v1Leaderboard().thenApply(this::toRatingStats);
  }

  private List<RatingStat> toRatingStats(List<LeaderboardEntry> entries) {
    Map<Integer, Long> totalCount = countByRating(entries.stream());
    Map<Integer, Long> countWithoutFewGames = countByRating(entries.stream()
        .filter(entry -> entry.gamesPlayedProperty().get() >= MINIMUM_GAMES_PLAYED_TO_BE_SHOWN));

    return totalCount.entrySet().stream()
        .map(entry -> new RatingStat(
            entry.getKey(),
            entry.getValue().intValue(),
            countWithoutFewGames.getOrDefault(entry.getKey(), 0L).intValue()))
        .collect(Collectors.toList());
  }

  private Map<Integer, Long> countByRating(Stream<LeaderboardEntry> entries) {
    return entries.collect(Collectors.groupingBy(leaderboardEntry ->
        RatingUtil.roundRatingToNextLowest100(leaderboardEntry.getRating()), Collectors.counting()));
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
