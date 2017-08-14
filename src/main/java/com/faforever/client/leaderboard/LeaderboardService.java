package com.faforever.client.leaderboard;

import com.faforever.client.game.KnownFeaturedMod;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface LeaderboardService {

  CompletableFuture<List<RatingStat>> getLadder1v1Stats();

  CompletableFuture<LeaderboardEntry> getEntryForPlayer(int playerId);

  CompletableFuture<List<LeaderboardEntry>> getEntries(KnownFeaturedMod ratingType);
}
