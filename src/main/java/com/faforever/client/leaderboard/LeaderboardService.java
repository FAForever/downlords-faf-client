package com.faforever.client.leaderboard;

import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.player.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface LeaderboardService {
  int MINIMUM_GAMES_PLAYED_TO_BE_SHOWN = 10;

  CompletableFuture<List<RatingStat>> getLadder1v1Stats();

  CompletableFuture<LeaderboardEntry> getEntryForPlayer(int playerId);

  CompletableFuture<List<LeaderboardEntry>> getEntries(KnownFeaturedMod ratingType);

  CompletableFuture<List<Player>> getPlayerObjectsById(String id);
}
