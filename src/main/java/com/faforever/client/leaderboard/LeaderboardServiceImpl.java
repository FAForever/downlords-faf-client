package com.faforever.client.leaderboard;

import com.faforever.client.FafClientApplication;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.remote.FafService;
import com.faforever.client.util.RatingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
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

  @Override
  public CompletableFuture<List<DivisionStat>> getDivisionStats() {
//    return getDivisions().thenAccept(divisions -> {
//      divisions.stream().map(division ->
//          fafService.getDivisionLeaderboard(division).thenApply(this::toDivisionStats))
//          .collect(Collectors.toList());
//    });
    return null;
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

  private DivisionStat toDivisionStats(List<LeaderboardEntry> entries) {
    return new DivisionStat(entries.size());
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
  public CompletableFuture<LeaderboardEntry> getLeagueEntryForPlayer(int playerId) {
    LeaderboardEntry entry = new LeaderboardEntry();
    entry.setSubDivisionIndex(4);
    entry.setMajorDivisionIndex(2);
    entry.setScore(8);
    return CompletableFuture.completedFuture(entry);
    //return fafService.getLeagueEntryForPlayer(playerId);
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

  @Override
  public CompletableFuture<List<Division>> getDivisions() {
    String[] subnames = {"V", "IV", "III", "II", "I"};
    String[] majornames = {"Bronze", "Silver", "Gold", "Diamond", "Master"};
    List<Division> divisions = new LinkedList<Division>();
    for (int k=1; k<6; k++) {
      for (int i=1; i<6; i++) {
        Division div = new Division(1, k, i, majornames[k-1], subnames[i-1], 10);
        //if (k!=5 || i!=5)
        divisions.add(div);
      }
    }
    Division div2 = new Division(1, 6, 1, "Supreme", "", 10);
    divisions.add(div2);
    return CompletableFuture.completedFuture(divisions);
    //return fafService.getDivisions();
  }

  @Override
  public CompletableFuture<List<LeaderboardEntry>> getDivisionEntries(Division division) {
    return fafService.getDivisionLeaderboard(division);
  }
}
