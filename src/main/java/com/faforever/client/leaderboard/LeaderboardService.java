package com.faforever.client.leaderboard;

import com.faforever.client.FafClientApplication;
import com.faforever.client.api.dto.GlobalRating;
import com.faforever.client.api.dto.GlobalRatingWithRank;
import com.faforever.client.api.dto.Rating;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.query.SearchablePropertyMappings;
import com.faforever.client.remote.FafService;
import com.faforever.client.util.RatingUtil;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import com.github.jasminb.jsonapi.JSONAPIDocument;
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
public class LeaderboardService {
  private final FafService fafService;
  public static final int MINIMUM_GAMES_PLAYED_TO_BE_SHOWN = 10;

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

  public CompletableFuture<LeaderboardEntry> getEntryForPlayer(int playerId) {
    return fafService.getLadder1v1EntryForPlayer(playerId);
  }

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

  public CompletableFuture<JSONAPIDocument<List<GlobalRatingWithRank>>> getSearchResultsWithMeta(KnownFeaturedMod ratingType, String nameToSearch, int page, int count) {
    switch (ratingType) {
      case FAF:
        return fafService.findGlobalLeaderboardEntryByQuery(nameToSearch, page, count);
      case LADDER_1V1:
        return null;
      default:
        throw new IllegalArgumentException("Not supported: " + ratingType);
    }
  }

  /*public CompletableFuture<List<LeaderboardEntry>> getSearchResults(KnownFeaturedMod ratingType, String nameToSearch, int page, int count) {
    switch (ratingType) {
      case FAF:
        return fafService.findGlobalLeaderboardEntryByQuery(nameToSearch, page, count);
      case LADDER_1V1:
        return fafService.findLadder1v1LeaderboardEntryByQuery(nameToSearch, page, count);
      default:
        throw new IllegalArgumentException("Not supported: " + ratingType);
    }

  }*/
}
